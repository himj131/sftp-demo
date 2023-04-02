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

import java.io.IOException;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.config.hosts.KnownHostEntry;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier;
import org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier.HostEntryPair;
import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.AuthorizedKeyEntry;
import org.apache.sshd.common.config.keys.PublicKeyEntryResolver;
import org.apache.sshd.common.util.GenericUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author Artem Bilan
 */
@Component
@Slf4j
public class SshSftpClient {

    private boolean allowUnknownKeys = true;
    private SshClient sshClient;
    private String userName = "test";
    private String password = "test123";
    private String host = "localhost";
    private Integer port = 22;

    private Resource privateKey;


    public ClientSession createSession() throws IOException, GeneralSecurityException {
        sshClient = SshClient.setUpDefaultClient();
        doInitClient();
        ConnectFuture connect = sshClient.connect(userName, host, port);
        return connect.verify().getSession();
    }

    private void doInitClient() throws IOException, GeneralSecurityException {
        ServerKeyVerifier serverKeyVerifier = getKeyVerifier();
        this.sshClient.setServerKeyVerifier(serverKeyVerifier);
        sshClient.setServerKeyVerifier(new ServerKeyVerifier() {
            @Override
            public boolean verifyServerKey(ClientSession clientSession, SocketAddress remoteAddress,
                PublicKey serverKey) {
                return true;
            }
        });
        this.sshClient.start();

        ClientSession clientSession = sshClient.connect(userName, host, port).getSession();
        clientSession.setAuthenticated();
    }

    private ServerKeyVerifier getKeyVerifier() throws IOException, GeneralSecurityException {
        ClassPathResource knownHostsResource = new ClassPathResource(
            "sftp/keys/sftp_known_hosts");
        Supplier<Collection<HostEntryPair>> keysSupplier = GenericUtils.memoizeLock(
            getKnownHostSupplier(knownHostsResource));
        return (ServerKeyVerifier) keysSupplier;
    }

    private static Supplier<Collection<KnownHostsServerKeyVerifier.HostEntryPair>> getKnownHostSupplier(
        ClassPathResource knownHostsResource) {
        return () -> {
            try {
                Collection<KnownHostEntry> entries =
                    KnownHostEntry.readKnownHostEntries(knownHostsResource.getInputStream(), true);
                List<KnownHostsServerKeyVerifier.HostEntryPair> keys = new ArrayList<>(
                    entries.size());
                for(KnownHostEntry entry : entries) {
                    keys.add(new KnownHostsServerKeyVerifier.HostEntryPair(entry,
                        resolveHostKey(entry)));
                }
                return keys;
            } catch(Exception ex) {
                log.warn("Known hosts cannot be loaded from the: " + knownHostsResource, ex);
                return Collections.emptyList();
            }
        };
    }

    private static PublicKey resolveHostKey(KnownHostEntry entry)
        throws IOException, GeneralSecurityException {
        AuthorizedKeyEntry authEntry = entry.getKeyEntry();
        Assert.notNull(authEntry, () -> "No key extracted from " + entry);
        return authEntry.resolvePublicKey(null, PublicKeyEntryResolver.IGNORING);
    }

}
