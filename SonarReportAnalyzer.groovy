/**
 * Licensed Materials - Property of IBM
 *
 * Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 **/
package com.ibm.curam.sonaranalyzer

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Input
import org.gradle.api.*
import groovy.json.JsonSlurper
import groovy.util.XmlSlurper

class SonarReportAnalyzer extends DefaultTask {
    @Input
    def sonarHtmlReport
    
    
    @TaskAction
    def analyzesonar() {
    	description 'Fail the build if new sonar issues are found'
 		if(sonarHtmlReport.exists()){
 		
 		boolean issue = false
			sonarHtmlReport.eachLine { line ->
				if(line.contains('\'new\': true, \'s\': \'blocker\'') || line.contains('\'new\': true, \'s\': \'critical\'')){
					issue = true
				}
			}
			
			if(issue){
				throw new GradleException('Fail Build - New Sonarqube BLOCKER or CRITICAL Issues are Found. \n Please read the sonarqube issues report in build/sonar/issues-report.')
			}
	   }else{
	   	println 'sonar-report is not exist.'
	   }
    }
}