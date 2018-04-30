package io.digitalstate.camunda;

import org.camunda.bpm.engine.impl.incident.DefaultIncidentHandler;
import org.camunda.bpm.engine.impl.incident.IncidentContext;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.IncidentEntity;
import org.camunda.bpm.engine.runtime.Incident;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;

import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;

import org.camunda.bpm.engine.impl.context.Context;
import java.io.InputStream;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.script.ScriptEngineManager;
import org.camunda.bpm.engine.impl.scripting.engine.ScriptEngineResolver;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;

import org.camunda.commons.utils.IoUtil;

public class NashornIncidentHandler extends DefaultIncidentHandler {

  private final static NashornIncidentHandlerLogger LOG = NashornIncidentHandlerLogger.LOG;
    static ScriptEngineManager manager = new ScriptEngineManager();
    static ScriptEngineResolver engine = new ScriptEngineResolver(manager);

  public NashornIncidentHandler(String type) {
    super(type);
  }

  @Override
  public String getIncidentHandlerType() {
    return super.getIncidentHandlerType();
  }

  @Override
  public Incident handleIncident(IncidentContext context, String message) {
    IncidentEntity incidentEntity = (IncidentEntity) super.handleIncident(context, message);

    prepareNashorn(context, message, "handleIncident");
    
    // @TODO add logic and notes
    return incidentEntity;
  }

  @Override
  public void resolveIncident(IncidentContext context) {
    super.resolveIncident(context);

    prepareNashorn(context, null, "resolveIncident");

  }

  @Override
  public void deleteIncident(IncidentContext context) {
    super.deleteIncident(context);

    prepareNashorn(context, null, "deleteIncident");

  }

  // Helper method to provide a ExecutionEntity for a executionId
  // Used to get the execution variable to get engine services and
  // pass execution into nashorn script
  private ExecutionEntity getExecution(String executionId) {
    if(executionId != null) {
      ExecutionEntity execution = Context.getCommandContext()
        .getExecutionManager()
        .findExecutionById(executionId);

      if (execution == null) {
        // LOG.executionNotFound(executionId);
      }

      return execution;
    }
    else {
      return null;
    }
  }


  /**
   * Gets all relevant data for execution of the Nashorn javascript script.
   * Determines which scripts to execute based on the extension properties 
   * defined in the BPMN process (or BPMN pool when there are 
   * multiple pools in a BPMN model/file).
   * 
   * This method uses the execution's deployment resources for finding 
   * the specific script file.
   * 
   * @param context the Incident Context that will be passed into the bindings.
   * @param message the Incident Message that will be passed into the bindings.
   * @param functionName the function name that will be passed into nashorn execution.
   */
  private void prepareNashorn(IncidentContext context, String message, String functionName){
    // Get the execution
    ExecutionEntity execution = this.getExecution(context.getExecutionId());
    
    BpmnModelInstance bpmnModel = execution.getBpmnModelInstance();
    String processKey = execution.getProcessEngineServices()
                                           .getRepositoryService()
                                           .getProcessDefinition(execution.getProcessDefinitionId())
                                           .getKey();

    Collection<Process> processList = bpmnModel.getModelElementsByType(org.camunda.bpm.model.bpmn.instance.Process.class);

    // Get the process instance that is currently being used in the instance
    Process process = processList.stream()
                                 .filter(p -> p.getId().equals(processKey))
                                 .findFirst()
                                 .get();
    LOG.debug("process-list-count", Integer.toString(processList.size()));

    // Get the process extension properties
    Collection<CamundaProperty> processProperties = process.getExtensionElements()
                                                           .getElementsQuery()
                                                           .filterByType(org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties.class)
                                                           .singleResult()
                                                           .getCamundaProperties()
                                                           .stream()
                                                           .filter(cp -> cp.getCamundaName().equals("incident_handler"))
                                                           .collect(Collectors.toList());
    LOG.debug("process-properties-incident_handler-count", Integer.toString(processProperties.size()));

    if (processProperties.isEmpty() == false) {
      // @TODO add better abstraction of bindings
      // Bindings bindings = nashornEngine.createBindings();
      Bindings bindings = new SimpleBindings();
      bindings.put("incidentContext", context);
      bindings.put("incidentMessage", message);
      bindings.put("execution", execution);

      // For Each Process Property that had the key "incident_handler"
      processProperties.forEach((property) -> {
        String fileName = property.getCamundaValue();
        LOG.info("incident-file-name", "Executing Nashorn incident handler filename: " + fileName);
        
        String processDefinitionId = execution.getProcessDefinitionId();
        String deploymentId = execution.getProcessEngineServices().getRepositoryService().getProcessDefinition(processDefinitionId).getDeploymentId();
        InputStream resource = execution.getProcessEngineServices().getRepositoryService().getResourceAsStream(deploymentId, fileName);

        this.executeNashornScript(resource, bindings, functionName);

      }); // End of For Each
    } else {
      LOG.info("No incident handler found", "No incident handler found in BPMN process");
    }
  }



  /**
   * Executes the nashorn script using the defined function name.
   * 
   * @param resource the javascript file to be read.
   * @param bindings the bindings that will be loaded into the script context.
   * @param functionName the function name that will be called for execution.
   * Typically is handleIncident, resolveIncident, or deleteIncident.
   */
  private void executeNashornScript(InputStream resource, Bindings bindings, String functionName) {

    // ScriptEngine is generated for each execution because if shared static engine 
    // is used, the running of mulitple incident handler scripts that have the same 
    // function names (such as handleIncident) are called multiple times (once for 
    // each incident handler defined in the process extensions)
    // @TODO look into better scripting isolation / script contexts? so the 
    // scriptEngine instance can be shared rather than having 
    // to be reloaded everytime.
    ScriptEngine nashornEngine = engine.getScriptEngine("nashorn", true);

    try {
      final String jsScript = IoUtil.inputStreamAsString(resource);

      // Setup a custom script context to execute the script in.
      // This ensures that share compilation does not occur, 
      // and the same named function does not execute multiple times
      final ScriptContext scriptCtxt = new SimpleScriptContext();
      
      // Small workaround because of weird buggy behaviour from ScriptEngine when 
      // setting custom context with bindings and using InvokeFunction
      scriptCtxt.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
      Bindings engineScope = scriptCtxt.getBindings(ScriptContext.ENGINE_SCOPE);

      // Compile script for later usage by invokeFunction
      final CompiledScript nashornCompiled = ((Compilable) nashornEngine).compile(jsScript);

      // Part of the Workaround described above, related to context, 
      // bindings, and InvokeFunction
      // NOTE: The .eval() will execute anything that is currently compiled, meaning that 
      // anything that is not wrapped in a function will execute.  
      // It is best practice to ensure that all code for IncidentHandler's 
      // are wrapper into their relevant functions (handleIncident, 
      // resolveIncident, and deleteIncident)
      nashornEngine.setContext(scriptCtxt);
      nashornCompiled.eval(engineScope);

      LOG.info("Nashorn-function-invoke", functionName);

      // Invoke the function name defined in the functionName variable.
      ((Invocable) nashornEngine).invokeFunction(functionName);

    } catch (ScriptException | NoSuchMethodException ex) {
        LOG.error("Nashorn-Error", "Cause: " + ex.getCause() 
                                              + ". Message: " 
                                              + ex.getMessage() 
                                              + ". Localized-Message: " 
                                              + ex.getLocalizedMessage());
    } // End of Catch
  }
} // End of Class