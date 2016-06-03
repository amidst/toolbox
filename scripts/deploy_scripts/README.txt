
SCOPE
=======
This script publish a new release for the toolbox. In short:
- Creates a new branch with the release
- Replaces most of the macros with the version (but for folder doc/ and README.md)
- Uploads the maven artefacts  


Note that it does not change the master branch.
The contain of the new release is obtained from the develop branch.


INSTALATION 
==============
For installing the deployment script, simply place the folder deploy_scripts into the parent
folder of the folder toolbox. That is, deploy_scripts and toolbox must be siblings folders.



SYSTEM REQUIREMENTS
======================
Make sure you have maven and git installed in your sysmtem.


Make sure you have your .m2 setting correctly set up for signing the artefacts. 
It should contain at least the profile with the private key and the github user. 
An example is given below (replace the content in brackets).

<settings>
  <profiles>
    <profile>
      <id>gpg</id>
      <properties>
        <gpg.executable>gpg2</gpg.executable>
        <gpg.passphrase>[amidst_key_pass]</gpg.passphrase>
      </properties>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>gpg</activeProfile>
  </activeProfiles>
    <servers>
        <server>
            <id>github</id>
            <username>[github_user]</username>
            <password>[github_pass]</password>
        </server>
    </servers>    
</settings>




USAGE
=========
The basic usage is:

./publishNewRelease.sh version mvn-args

For example 

./publishNewRelease.sh 1.5.3 -Dmaven.test.skip=true 

