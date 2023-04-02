/*
 * Copyright 2014-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hi.mj.sfpt;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class SftpServer implements InitializingBean, SmartLifecycle {

    public static final int PORT = 52222;
    public static final String HOST = "localhost";
    public static final String AUTH_USER = "user";
    public static final String AUTH_PASSWORD = "password";

    private final SshServer server = SshServer.setUpDefaultServer();


    private volatile boolean running;

    private DefaultSftpSessionFactory defaultSftpSessionFactory;

//    public SftpServer(int port, DefaultSftpSessionFactory defaultSftpSessionFactory) {
//        this.port = port;
//        this.defaultSftpSessionFactory = defaultSftpSessionFactory;
//    }


    @Override
    public void afterPropertiesSet() throws Exception {
        this.server.setPort(SftpServer.PORT);
        this.server.setHost(SftpServer.HOST);
        this.server.setKeyPairProvider(
            new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath()));
        this.server.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
        this.server.setPublickeyAuthenticator(getPublicKeyAuthenticator());
//        final PublicKey allowedKey = KeyUtils.decodePublicKey("sftp/keys/sftp_known_hosts");
//        this.server.setPublickeyAuthenticator((username, key, session) -> key.equals(allowedKey));
        this.server.setPasswordAuthenticator(new PasswordAuthenticator() {
            @Override
            public boolean authenticate(String username, String password, ServerSession session)
                throws PasswordChangeRequiredException, AsyncAuthException {
                return username.equals(AUTH_USER) && password.equals(AUTH_PASSWORD);
            }
        });
        final String pathname =
            System.getProperty("java.io.tmpdir") + "sftptest" + File.separator;
        new File(pathname).mkdirs();
        server.setFileSystemFactory(new VirtualFileSystemFactory(Paths.get(pathname)));
    }


    private PublickeyAuthenticator getPublicKeyAuthenticator() throws Exception {
        Path path = new ClassPathResource("sftp/keys/sftp_known_hosts").getFile().toPath();
        return new AuthorizedKeysAuthenticator(path);
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void start() {
        try {
            this.server.setPort(PORT);
            this.server.start();
//            this.defaultSftpSessionFactory.setPort(this.server.getPort());
            this.running = true;
            System.out.println("sftp server started");
        } catch(IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public void stop() {
        if(this.running) {
            try {
                server.stop(true);
            } catch(Exception e) {
                throw new IllegalStateException(e);
            } finally {
                this.running = false;
            }
        }
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }
}
