var system = java.lang.System

function handleIncident(){
  system.out.println("This is a handleIncident Function Message: ");
  system.out.println(incidentContext.getProcessDefinitionId())
  system.out.println(incidentMessage)

}



// incidentContext.getProcessDefinitionId()
// incidentContext.getActivityId()
// incidentContext.getExecutionId()
// incidentContext.getConfiguration()
// incidentContext.getTenantId()
// incidentContext.getJobDefinitionId()

// execution variable is available

// incidentMessage is also available


// example output
// camunda_1  | This is a message we wanted to see!!
// camunda_1  | myProcess-incident1:4:2c726ce0-4b60-11e8-8225-0242ac130002
// camunda_1  | ENGINE-09027 Exception while resolving duedate 'dog': Invalid format: "dog"