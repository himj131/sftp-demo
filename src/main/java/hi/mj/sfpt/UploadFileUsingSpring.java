package hi.mj.sfpt;

import java.io.IOException;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class UploadFileUsingSpring {

    public static void main(String[] args) throws IOException {

        DefaultSftpSessionFactory f = new DefaultSftpSessionFactory();
        f.setHost("localhost");
        f.setPort(56486);
        f.setUser("user");
        f.setAllowUnknownKeys(true);
//        f.setKnownHostsResource(new ClassPathResource("sftp/keys/sftp_known_hosts"));
//        f.setPrivateKeyPassphrase(passphrase);
        f.setPassword("pass");
        Session<SftpClient.DirEntry> session2 = f.getSession();
        System.out.println(session2.getHostPort());


    }
}
