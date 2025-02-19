void buildImage() {
    sh """
        cd build/rpmbuild-docker
        sg docker -c "
            docker build --pull --squash --tag public.ecr.aws/e7j3v3n0/rpmbuild:2 .
        "
    """
    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: 'ECRRWUser', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
        sh """
            aws ecr-public get-login-password --region us-east-1 | docker login -u AWS --password-stdin public.ecr.aws/e7j3v3n0
            sg docker -c "
                docker push public.ecr.aws/e7j3v3n0/rpmbuild:2
            "
        """
    }
}
pipeline {
    agent {
        label 'docker'
    }
    options {
        skipStagesAfterUnstable()
        buildDiscarder(logRotator(artifactNumToKeepStr: '10'))
    }
    stages {
        stage('Prepare') {
            steps {
                git poll: true, branch: 'PMM-2.0', url: 'https://github.com/Percona-Lab/pmm-submodules.git'
                sh '''
                    git reset --hard
                    git clean -xdf
                '''
            }
        }
        stage('Build') {
            steps {
                buildImage()
            }
        }
    }
}
