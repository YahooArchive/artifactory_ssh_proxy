package com.yahoo.sshd.authorization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.authorization.file.FileBasedArtifactoryAuthorizer;
import com.yahoo.sshd.server.settings.SshdSettingsInterface;

/**
 * Added this to make doing dev work a lot less painful. I don't want to setup an auth.txt for doing local development,
 * so add a flag. The problem is, we probably should flag this separately, because later we might create a developer
 * version that hosts more bits. When that is done, we'll have to rethink this.
 * 
 * @author areese
 * 
 */
public class WideOpenGatesAuthrorizer implements ArtifactoryAuthorizer {
    private static final Logger LOG = LoggerFactory.getLogger(FileBasedArtifactoryAuthorizer.class);

    public WideOpenGatesAuthrorizer(SshdSettingsInterface settings) {

    }

    @Override
    public boolean authorized(String repositoryName, String userName, ArtifactoryPermTargetType permissionTarget) {
        LOG.info("Development mode, blindly allowing user: '{}' access to repo: '{}' with target '{}", userName,
                        repositoryName, permissionTarget);
        return true;
    }

}
