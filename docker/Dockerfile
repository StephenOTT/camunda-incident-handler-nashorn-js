FROM camunda/camunda-bpm-platform:tomcat-7.8.0

# add custom configurations
COPY docker/camunda/conf/ /camunda/conf

# add JS script for RSA encryption
COPY docker/camunda/lib/digitalstate.camunda.nashorn.incident.handler-0.1.0-SNAPSHOT.jar /camunda/lib/digitalstate.camunda.nashorn.incident.handler-0.1.0-SNAPSHOT.jar