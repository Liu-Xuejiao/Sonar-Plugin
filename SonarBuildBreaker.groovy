import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Input
import org.gradle.api.*
import java.net.HttpURLConnection
import groovy.json.JsonSlurper

class SonarBuildBreaker extends DefaultTask {
    @Input
    def reportPath

    @TaskAction
    def analyzesonar() {

      if (!reportPath.exists()){
          println 'Sonarqube run in PREVIEW model.'
      } else {
          def properties = new Properties()
          reportPath.withInputStream {
                  properties.load(it)
          }
          def taskUrl = new URL(properties.ceTaskUrl)
          def jsonParser = new JsonSlurper()
          def jenkinsUser = "jenkins"
          def jenkinsPass = "Password-placeholder"
          def status = "PENDING"
          String analysisId = null
          
          def authString = "${jenkinsUser}:${jenkinsPass}".getBytes().encodeBase64().toString()
          HttpURLConnection conn = (HttpURLConnection)taskUrl.openConnection()
          conn.setRequestProperty( "Authorization", "Basic ${authString}" )
 
          if( conn.responseCode == 200 ) {
              while (status == "PENDING" || status == "IN_PROGRESS") {
                  def response = jsonParser.parse(conn.getContent())
                  status = response.task.status
                  analysisId = response.task.analysisId
                  conn = (HttpURLConnection)taskUrl.openConnection()
                  conn.setRequestProperty( "Authorization", "Basic ${authString}" )
                  sleep(1000)
              }
      
              if (status == "FAILED" || status == "CANCELED") {
                   throw new GradleException('Sonarqube check failed. 1 ')
              }
              else if (status == "SUCCESS") {
                  def analysisUrl = new URL("${properties.serverUrl}/api/qualitygates/project_status?analysisId=${analysisId}")
                  def analysisConn = analysisUrl.openConnection()
                  analysisConn.setRequestProperty( "Authorization", "Basic ${authString}" )
                  
                  if( analysisConn.responseCode == 200 ) {
                      def result = jsonParser.parse(analysisConn.getContent())
                      if (result.projectStatus.status == "OK") {
                           println 'Pass the quality gate associated with the analysis. Sonarqube check sucessfully.'
                      } else if (result.projectStatus.status == "ERROR") {
                           throw new GradleException('Fail the quality gate associated with the analysis')
                      } else {
                           println 'There is no quality gate associated with the analysis. Sonarqube check sucessfully.'
                      }
                  } else {
                      println "Something bad happened."
                      println "${analysisConn.responseCode}: ${analysisConn.responseMessage}"
                  }
              } else {
                   throw new GradleException('Sonarqube check failed. 2 ')
              }
          } else {
            println "Something bad happened."
            println "${conn.responseCode}: ${conn.responseMessage}"
          }
          
      }
   }
}
