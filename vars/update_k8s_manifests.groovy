def call(Map config = [:]) {

    def imageTag      = config.imageTag ?: "latest"
    def manifestsPath = config.manifestsPath ?: "kubernetes"
    def gitCredentials = config.gitCredentials ?: "github-credentials"
    def gitUserName   = config.gitUserName ?: "jenkins"
    def gitUserEmail  = config.gitUserEmail ?: "jenkins@example.com"
    def gitBranch     = config.gitBranch ?: "master"

    withCredentials([
        usernamePassword(
            credentialsId: gitCredentials,
            usernameVariable: 'GIT_USERNAME',
            passwordVariable: 'GIT_PASSWORD'
        )
    ]) {

        sh """
            set -e

            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"

            git clean -fd
            git reset --hard

            echo "Updating manifests with image tag: ${imageTag}"

            #################################################
            # Update main application image
            #################################################
            sed -i "s#image: .*easyshop-app:.*#image: akshayaws99/easyshop-app:${imageTag}#g" \
            ${manifestsPath}/08-easyshop-deployment.yaml


            #################################################
            # Update migration image if file exists
            #################################################
            if [ -f ${manifestsPath}/12-migration-job.yaml ]; then
                sed -i "s#image: .*easyshop-migration:.*#image: akshayaws99/easyshop-migration:${imageTag}#g" \
                ${manifestsPath}/12-migration-job.yaml
            fi


            #################################################
            # Update ingress host if file exists
            #################################################
            if [ -f ${manifestsPath}/10-ingress.yaml ]; then
                sed -i "s|host: .*|host: easyshop.letsdeployit.com|g" \
                ${manifestsPath}/10-ingress.yaml
            fi


            #################################################
            # Show what changed
            #################################################
            echo "==== Git Diff ===="
            git diff ${manifestsPath} || true


            #################################################
            # Commit and push only if manifests changed
            #################################################
            if ! git diff --quiet; then
                git add ${manifestsPath}/*.yaml

                git commit -m "🔧 Updating Kubernetes manifests with image tag: ${imageTag}"

                git push https://\${GIT_USERNAME}:\${GIT_PASSWORD}@github.com/${gitUserName}/tws-e-commerce-app_hackathon.git HEAD:${gitBranch}

                echo "Manifests updated and pushed successfully."
            else
                echo "No manifest changes detected. Skipping commit and push."
            fi
        """
    }
}
