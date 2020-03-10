timestamps {
    node('maven') {
        stage('Checkout'){
            //checkout scm
            checkout([$class: 'GitSCM', branches: [[name: '*/aks']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/cmotta2016/gs-spring-boot.git']]])
        }
        stage('Compile with Azure Artifacts'){
	        sh 'mvn -gs /home/jenkins/.m2/settings.xml clean install'
        }
        stage ('Code Quality'){
	        withSonarQubeEnv('SonarQube') { 
	        sh 'mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.6.0.1398:sonar'
	        }
        }
        stage('Quality Gate'){
            sleep(20)
            timeout(activity: true, time: 20, unit: 'SECONDS') {
                def qg = waitForQualityGate()
                if (qg.status != 'OK') {
                    error "Pipeline aborted due to quality gate failure: ${qg.status}"
                }
            }
        }
        stage('Build with S2I'){
            sh 's2i build . cmotta2016/java-8-bases2i:latest cmotta2016.azurecr.io/maven:${BUILD_NUMBER} --loglevel 5 --network host --inject /opt/artifacts:/opt/jboss/container/maven/default'
        }
        stage('Push Image to ACR'){
            withCredentials([usernamePassword(credentialsId: 'acr-credentials', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
            sh '''
            docker login -u "$DOCKER_USERNAME" -p "$DOCKER_PASSWORD" cmotta2016.azurecr.io
            docker tag cmotta2016.azurecr.io/maven:${BUILD_NUMBER} cmotta2016.azurecr.io/maven:latest
            docker push cmotta2016.azurecr.io/maven:${BUILD_NUMBER}
            docker push cmotta2016.azurecr.io/maven:latest
            docker rmi -f cmotta2016.azurecr.io/maven:${BUILD_NUMBER} cmotta2016.azurecr.io/maven:latest
            '''
            }
        }
        stage('Deploy QA'){
            sh 'kubectl config set-context --current --namespace=java-qa'
            def deployment = sh(script: "kubectl get deployment maven -o jsonpath='{ .metadata.name }' --ignore-not-found", returnStdout: true).trim()
            if (deployment == "maven") {
                sh 'kubectl set image deployment/maven maven=cmotta2016.azurecr.io/maven:${BUILD_NUMBER} --record'
            }
            else {
                sh 'kubectl create -f aks-maven.yaml --validate=false -l env!=hml'
                sh 'kubectl set image deployment/maven maven=cmotta2016.azurecr.io/maven:${BUILD_NUMBER} --record'
            }
            sh 'kubectl rollout status deployment.apps/maven'
        }
        /*stage('Promote to HML'){
            //routeHost = sh(script: "kubectl get ingress maven -n maven-qa -o jsonpath='{ .spec.rules[0].host }'", returnStdout: true).trim()
            input message: "Promote to HML. Approve?", id: "approval"
        }*/
        stage('Test Deployment'){
            routeHost = sh(script: "kubectl get ingress maven-o jsonpath='{ .spec.rules[0].host }'", returnStdout: true).trim()
            input message: "Test deployment: http://${routeHost}. Approve?", id: "approval"
        }
        /*stage('Deploy HML'){
	    sh 'kubectl config set-context --current --namespace=java-hml'
            def deployment = sh(script: "kubectl get deployment maven -o jsonpath='{ .metadata.name }' --ignore-not-found", returnStdout: true).trim()
            if (deployment == "maven") {
                sh 'kubectl set image deployment/maven maven=cmotta2016.azurecr.io/gs-maven:${BUILD_NUMBER} --record'
            }
            else {
                sh "kubectl create -f aks-maven.yaml --validate=false --dry-run -o yaml | sed 's/qa/hml/g' | kubectl apply --validate=false -f -"
                sh 'kubectl set image deployment/maven maven=cmotta2016.azurecr.io/gs-maven:${BUILD_NUMBER} --record'
            }
            //sh 'kubectl wait --for=condition=Available deployment/maven -n maven --timeout=90s'
            sh 'kubectl rollout status deployment.apps/maven'
        }
        stage('Test Deployment'){
            routeHost = sh(script: "kubectl get ingress maven -o jsonpath='{ .spec.rules[0].host }'", returnStdout: true).trim()
            input message: "Test deployment: http://${routeHost}. Approve?", id: "approval"
        }*/
    }
}
