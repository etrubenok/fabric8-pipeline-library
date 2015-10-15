def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def project = 'fabric8-ipaas'
  node ('swarm'){
    ws (project){
      withEnv(["PATH+MAVEN=${tool 'maven-3.3.1'}/bin"]) {

        def flow = new io.fabric8.Release()
        flow.setupWorkspace (project)

        def uid = UUID.randomUUID().toString()

        sh "git checkout -b versionUpdate${uid}"
        def fabric8Version = flow.getReleaseVersion "fabric8-maven-plugin"
        def updated = false
        try {
          flow.searchAndReplaceMavenVersionProperty("<fabric8.version>", fabric8Version)
          updated = true
        } catch (err) {
          echo "Already on the latest versions of fabric8 dependencies"
        }

        try {
          def parsedVersion = fabric8Version.split('\\.')
          def nextFabric8DevelopmentSnapshotVersion = (parsedVersion[2].toInteger() + 1)
          flow.searchAndReplaceMavenSnapshotProfileVersionProperty("<fabric8.version>", parsedVersion[0] + '.' + parsedVersion[1] + '.' + nextFabric8DevelopmentSnapshotVersion)
          updated = true
        } catch (err) {
          echo "Already on the latest SNAPSHOT versions of fabric8 dependencies"
        }
        // only make a pull request if we've updated a version
        if (updated) {
          sh "git push origin versionUpdate${uid}"
          return flow.createPullRequest("[CD] Update release dependencies")
        } else {
          message = "fabric8-ipaas already on the latest release versions"
          hubot room: 'release', message: message
          return
        }
      }
    }
  }
}