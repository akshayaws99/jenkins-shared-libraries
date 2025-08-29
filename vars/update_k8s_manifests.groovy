def call(Map config = [:]) {
    def imageTag = config.imageTag ?: "latest"
    def manifestsPath = config.manifestsPath ?: "kubernetes"
    def gitCredentials = config.gitCredentials ?: "github-credentials"
    def gitUserName = config.gitUserName ?: "jenkins"
    def gitUserEmail = config.gitUserEmail ?: "jenkins@example.com"

    withCredentials([usernamePassword(credentialsId: gitCredentials,
                                      usernameVariable: 'GIT_USERNAME',
                                      passwordVariable: 'GIT_PASSWORD')]) {
        sh """
            set -e
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"
            git clean -fd
            git reset --hard

            # Update app image
            sed -i "s|image: akshayaws99/easyshop-app:.*|image: akshayaws99/easyshop-app:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml

            # Update migration image if file exists
            if [ -f ${manifestsPath}/12-migration-job.yaml ]; then
                sed -i "s|image: akshayaws99/easyshop-migration:.*|image: akshayaws99/easyshop-migration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi

            # Update ingress host if file exists
            if [ -f ${manifestsPath}/10-ingress.yaml ]; then
                sed -i "s|host: .*|host: easyshop.letsdeployit.com|g" ${manifestsPath}/10-ingress.yaml
            fi

            git add ${manifestsPath}/*.yaml
            git commit -m "🔧 Updating Kubernetes manifests with image tag: ${imageTag}" || echo "No changes to commit"
            git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/${gitUserName}/tws-e-commerce-app_hackathon.git HEAD:${config.gitBranch ?: "master"}
        """
    }
}
