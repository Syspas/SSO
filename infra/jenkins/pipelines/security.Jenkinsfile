pipeline {
    agent any
    options {
        timeout(time: 45, unit: 'MINUTES')
        disableConcurrentBuilds()
    }
    environment {
        SOURCE_DIR = '/home/master/IdeaProjects/sso'
    }
    stages {
        stage('Подготовка') {
            steps {
                deleteDir()
                sh '''
                    set -eu
                    test -r "${SOURCE_DIR}/pom.xml"
                    rsync -a --delete \
                      --exclude target --exclude .idea --exclude .cursor --exclude .git \
                      --exclude .allure --exclude 'ajcore.*.txt' \
                      "${SOURCE_DIR}/" "${WORKSPACE}/"
                    chmod +x ./mvnw ./scripts/*.sh
                    ./mvnw -version
                '''
            }
        }
        stage('Безопасность') {
            steps {
                sh './scripts/test-security.sh'
            }
        }
    }
    post {
        always {
            script {
                if (sh(script: 'ls target/surefire-reports-security/*.xml >/dev/null 2>&1', returnStatus: true) == 0) {
                    junit testResults: 'target/surefire-reports-security/*.xml'
                }
                if (fileExists('target/allure-results')) {
                    allure([
                        includeProperties: false, jdk: '', commandline: 'allure',
                        properties: [], reportBuildPolicy: 'ALWAYS',
                        results: [[path: 'target/allure-results']],
                        report: 'target/allure-report'
                    ])
                    sh 'jar --create --no-manifest --file allure-report-security.zip -C target/allure-report .'
                    archiveArtifacts artifacts: 'allure-report-security.zip', allowEmptyArchive: false
                }
            }
        }
    }
}
