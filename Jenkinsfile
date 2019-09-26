timestamps{
    node('maven'){
        stage('Checkout'){
           //checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/cmotta2016/gs-spring-boot.git']]])
           checkout scm
        }
        stage('Cleanup'){
            sh 'oc delete all -l app=maven -n maven-backend-qa'
            sh 'oc delete pvc -l app=maven -n maven-backend-qa'
         }
        stage('Compile'){
            sh 'mvn clean install'
        }
        stage('Code Quality'){
            withSonarQubeEnv('SonarQube') { 
                sh 'mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.6.0.1398:sonar'
            }
        }
        stage('Quality gate'){
            timeout(activity: true, time: 15, unit: 'SECONDS') {
                sleep(10)
                def qg = waitForQualityGate()
                if (qg.status != 'OK') {
                    error "Pipeline aborted due to quality gate failure: ${qg.status}"
                }
            }
        }
        stage('Build'){
            sh 'oc new-build --name=maven-spring openshift/java --binary=true -l app=maven -n maven-backend-qa'
            sh 'oc start-build maven-spring --from-dir=target --follow -n maven-backend-qa'
        }
        stage('Deploy'){
            sh "oc new-app --file=template-maven.yml --param=LABEL=maven --param=NAME=maven-spring --namespace=maven-backend-qa"
        }
    }
}
