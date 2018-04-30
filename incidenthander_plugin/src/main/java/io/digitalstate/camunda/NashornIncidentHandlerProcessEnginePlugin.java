package io.digitalstate.camunda;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.bpm.engine.impl.incident.IncidentHandler;

public class NashornIncidentHandlerProcessEnginePlugin implements ProcessEnginePlugin {
  
  private String incidentTypes;
  private final static NashornIncidentHandlerLogger LOG = NashornIncidentHandlerLogger.LOG;

  @Override
  public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    LOG.info("plugin-startup", "Adding Nashorn Incident Handler Process Engine Plugin");
    
    List<String> incidentTypesList = new ArrayList<String>(Arrays.asList(incidentTypes.split(",")));
    List<IncidentHandler> incidentHandlers = new ArrayList<IncidentHandler>();

    incidentTypesList.forEach(i -> {
      IncidentHandler incidentHandler = (IncidentHandler) new NashornIncidentHandler(i);
      incidentHandlers.add(incidentHandler);
    });

    // processEngineConfiguration.setCustomIncidentHandlers(Arrays.asList((IncidentHandler) new NashornIncidentHandler("failedJob")));
    processEngineConfiguration.setCustomIncidentHandlers(incidentHandlers);
    processEngineConfiguration.getCustomIncidentHandlers().forEach(ih->{
      LOG.info("incidenthander-type", "Added Incident Handler Type: " + ih.getIncidentHandlerType());
    });
   }

  @Override
  public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
 
  }

  @Override
  public void postProcessEngineBuild(ProcessEngine processEngine) {

  }

  /**
   * @param types Sets list of incident types that will be used for plugin execution
  */
  public void setIncidentTypes(String types) {
      this.incidentTypes = types;
  }



}