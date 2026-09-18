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
                      --exclude target \
                      --exclude .idea \
                      --exclude .cursor \
                      --exclude .git \
                      --exclude .allure \
                      --exclude 'ajcore.*.txt' \
                      "${SOURCE_DIR}/" "${WORKSPACE}/"
                    chmod +x ./mvnw
                    chmod +x ./scripts/*.sh
                    java -version
                    ./mvnw -version
                '''
            }
        }
        stage('Юнит-тесты') {
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    sh './scripts/test-unit.sh'
                }
            }
        }
        stage('Безопасность') {
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    sh './scripts/test-security.sh'
                }
            }
        }
        stage('UI (HtmlUnit)') {
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    sh './scripts/test-ui.sh'
                }
            }
        }
        stage('Архитектура') {
            steps {
                sh './scripts/test-architecture.sh'
            }
        }
        stage('Смоук jar') {
            when {
                expression { currentBuild.currentResult == 'SUCCESS' }
            }
            steps {
                sh './scripts/test-smoke-boot.sh'
            }
        }
    }

    post {
        always {
            script {
                def reports = []
                if (fileExists('target/surefire-reports')) {
                    reports.add('target/surefire-reports/*.xml')
                }
                if (fileExists('target/surefire-reports-security')) {
                    reports.add('target/surefire-reports-security/*.xml')
                }
                if (fileExists('target/surefire-reports-architecture')) {
                    reports.add('target/surefire-reports-architecture/*.xml')
                }
                if (fileExists('target/surefire-reports-ui')) {
                    reports.add('target/surefire-reports-ui/*.xml')
                }
                if (!reports.isEmpty()) {
                    junit testResults: reports.join(',')
                }
                if (fileExists('target/allure-results')) {
                    allure([
                        includeProperties: false,
                        jdk: '',
                        commandline: 'allure',
                        properties: [],
                        reportBuildPolicy: 'ALWAYS',
                        results: [[path: 'target/allure-results']],
                        report: 'target/allure-report'
                    ])
                    archiveArtifacts artifacts: 'target/allure-report/**', allowEmptyArchive: true
                    echo "Allure Report: ${env.BUILD_URL}allure/"
                }
            }
        }
    }
}
