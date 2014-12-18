# Artifactory sshd_proxy

**artifactory_ssh_proxy** is an Artifactory SSH proxy that lets you `scp` files directly into your Artifactory server.  No need to use the REST API.

You will be able to push artifacts up as simply as:

`$ scp artifact_file your-artifactory-host.org:/repository-name/directory-name/your_artifact`


If you are using maven, you can one-line deploy artifacts with:

`$ mvn deploy -DaltDeploymentRepository=foo::default::scpexe://your-artifactory-ssh-proxy-host.org:your-ssh-port/repository-name/`

If you do this, be sure to include wagon-ssh-external artifact in your project's `pom.xml`:

```
<build>
  <extensions>
    <extension>
      <groupId>org.apache.maven.wagon</groupId>
      <artifactId>wagon-ssh-external</artifactId>
      <version>1.0-beta-6</version>
    </extension>
  </extensions>
</build>
```


## Requirements

- Currently the proxy will only run on a linux machine.  Sorry windows.
- Java 1.7 installed and configured.
   - JCE crypto from: `http://www.oracle.com/technetwork/java/embedded/embedded-se/downloads/jce-7-download-432124.html` or `http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html`
- An artifactory server for which you have created an account (username & password) for the proxy to use.
- Anyone who wants to use the proxy will need to be capable of ssh'ing into the proxy server.
   - FIXME: should be able to seperate this out.

- It's highly advisable to look at apache commons daemon to run this while dropping privileges.
- For unix operating systems, jsvc: http://commons.apache.org/proper/commons-daemon/jsvc.html 
- For windows, procrun: http://commons.apache.org/proper/commons-daemon/procrun.html

## Build & Run

### Setup

1. Be sure to have Java 1.7 or later, and and Maven 3 installed.
2. On your Artifactory instance/server, create a user for the proxy.  You will use this user name and password in a properties file (next step).
3. Create a properties file, `sshd_proxy.properties`, and put it in: `/opt/sshd_proxy/conf/sshd_proxy/`.
4. In the `sshd_proxy.properties`, write the following 4 lines, replace the examples here with your actual values:

    ```
    sshd.port=2222
    sshd.artifactoryUrl=http://your_artifactory_host:4080/artifactory
    sshd.artifactoryUsername=artifactory_username
    sshd.artifactoryPassword=artifactory_password
    sshd.artifactoryAuthorizationFilePath=/opt/sshd_proxy/conf/sshd_proxy/auth/auth.txt
    ```

5. The proxy authentication works by checking against an `auth.txt` file to determine the permissions of each user who wants to use artifactory.  This file has to be created at:  `/opt/sshd_proxy/conf/sshd_proxy/auth/auth.txt`.
6. In `auth.txt`, format the file like this example, replacing the repo names with actual repos, and the user names with actual     users who are on the proxy machine:

    ```
    # add repository names and users
    # permissions are separated by commas
    # users are separated by pipe
    # * represents all

    # user areese and user jweaver can write, all users can read.
    repo-foo-bar=WRITE:areese|jweaver,READ:*

    # all writes and reads allowed in the repo "repo-open-to-all"
    repo-open-to-all=WRITE:*,READ:*
    ```

7. The proxy server is designed to be used in large scale deployments with many proxy servers under a load balancer.  In this production situation, the hostkey needs to match amongst all the individual proxy servers.  For now, lets assume a single machine instance and symlink the hostkey where `sshd_proxy` looks.  _Prior to doing this step_, you will want to create an ssh key (DSA) for the user account that runs the proxy jar.
    A host key can be created by running `ssh-keygen -t dsa -f $ROOT/conf/sshd_proxy/ssh_host_dsa_key -C '' -N ''`

    `$ sudo ln -s ~/.ssh/ssh_host_dsa_key /opt/sshd_proxy/conf/sshd_proxy/ssh_host_dsa_key`

8. In a production environment, you will want to lock down access to the ssh host key, for now we will assume a local test box (single proxy machine).  Open up access outside of root so the user running the jar can access the dsa host key:

    `$ sudo chmod 644 /opt/sshd_proxy/conf/sshd_proxy/ssh_host_dsa_key`

9. The proxy writes log files out to `/opt/sshd_proxy/logs/sshd_proxy`.
    1. Create the log directory:  `$ sudo mkdir -p /opt/sshd_proxy/logs/sshd_proxy`
    2. Then change the permissions: `$ sudo chmod 750 /opt/sshd_proxy/logs/sshd_proxy`  
    3. Change the owner: `$ sudo chown <logs_user> /opt/sshd_proxy/logs/sshd_proxy`

10. Now you should be all set!  Go ahead and run it.

## Run

First ensure you have the jce installed correctly:

1.  Build the jar if you haven't already:  `mvn clean install`
2.  Get your classpath:  `mvn dependency:build-classpath -Dmdep.outputFile=target/sshd_classpath`
3.  ``java -cp target/sshd_proxy-0.2.0-SNAPSHOT.jar:`cat target/sshd_classpath` com.yahoo.sshd.server.TestCiphers 2>&1  | grep length``  this should output 7.

To run the proxy locally, do the following:

1.  Create a config and an auth file.
    1. sh create_config.sh will create one for you.
    1. -x below negates the need for an auth file.
2.  Build the jar if you haven't already:  `mvn clean install`
3.  Get your classpath:  `mvn dependency:build-classpath -Dmdep.outputFile=target/sshd_classpath`
4.  Run the proxy:  ``java -cp target/sshd_proxy-0.2.0-SNAPSHOT.jar:`cat target/sshd_classpath` com.yahoo.sshd.server.Sshd -r developer_config -x``
    `-x` disables the auth.txt configuration.
    Currently it wants to scan /home for authorized_keys, this is broken on a mac.

## Misc notes.

The sshd proxy embeds jetty to allow custom functionality.
If you have a vip that performs status checks over http, you can use jetty to communicate back to the vip.

If you are running multiple instances of the proxy behind a vip, you'll want to ensure that the host keys 
of all hosts that are behind the vip match.

Currently the only functionality supported is scp, and it does not do checksum based uploads yet.
This means that for every upload, it will upload the entire contents of the file.

If you have a long trip to your artifactory server, you might get timeout's with large file uploads
via the proxy.

The shell functionality only returns an informational message.
You will still need to run sshd on port 22 to manage your machine, this proxy is only for proxying to artifactory
and does not replace sshd.

The informational message allows you to do troubleshooting of keys by having a user ssh to the port the proxy is on
and that will verify that the proxy is aware of and is accepting their keys.
This also allows you to separate the people who can upload from the people who manage the actual machine.

