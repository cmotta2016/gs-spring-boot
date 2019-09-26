timestamps{
    node('mavenoci'){
        stage('Checkout'){
           //checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/cmotta2016/gs-spring-boot.git']]])
            checkout scm
        }
        /*stage('Cleanup'){
            sh 'oc delete all -l app=maven -n maven-backend'
            sh 'oc delete pvc -l app=maven -n maven-backend'
         }*/
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
        stage('Build S2I'){
            /*sh 'oc new-build --name=maven-spring openshift/java --binary=true -l app=maven -n maven-backend'
            sh 'oc start-build maven-spring --from-dir=target --follow -n maven-backend'*/
            sh 's2i build . fabric8/java-main cmotta2016/k8s-maven --loglevel 1 --network host'
        }
        /*stage('Deploy'){
            sh "oc new-app --file=template-maven.yml --param=LABEL=maven --param=NAME=maven-spring --namespace=maven-backend"
        }*/
        stage('Push Image'){
            withCredentials([usernamePassword(credentialsId: 'docker-io', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
            sh '''
            docker login -u "$DOCKER_USERNAME" -p "$DOCKER_PASSWORD"
            docker push cmotta2016/k8s-maven
            docker rmi -f cmotta2016/k8s-maven
            '''
            }
        }
        stage('Deploy'){
            sh 'kubectl apply -f maven-template.yml -n maven'
        }        
    }
}
