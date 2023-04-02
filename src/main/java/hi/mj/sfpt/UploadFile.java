package hi.mj.sfpt;

import java.io.IOException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.SftpVersionSelector;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class UploadFile {

    public static void main(String[] args) throws IOException {
        SshClient client = SshClient.setUpDefaultClient();

        // Open the client
        client.start();

        // Connect to the server
        ClientSession clientSession = client.connect(SftpServer.AUTH_USER, SftpServer.HOST,
            SftpServer.PORT).verify().getSession();
        clientSession.addPasswordIdentity(SftpServer.AUTH_PASSWORD);
        clientSession.auth().verify();
        SftpClient sftpClient = SftpClientFactory.instance().createSftpClient(clientSession,
            SftpVersionSelector.CURRENT);
//        ClientSession session = sftpClient.getSession();
// 쓰기
//        ClassPathResource resource = new ClassPathResource("sftp/sample/test");
//        InputStream inputStream = resource.getInputStream();
//        String destination = "destination";
//        synchronized(sftpClient) {
//            sftpClient.mkdir(destination);
//            OutputStream outputStream = sftpClient.write(
//                destination + "/" + resource.getFilename());
//            FileCopyUtils.copy(inputStream, outputStream);
//        }

        //읽기
//        String source = "destination/test";
//        InputStream is = sftpClient.read(source);
//
//        BufferedOutputStream outputStream = new BufferedOutputStream(new ByteArrayOutputStream());
//        FileCopyUtils.copy(is, outputStream);

        String path = "/destination";
        String remotePath = StringUtils.trimTrailingCharacter(path, '/');
        String remoteDir = remotePath;
        int lastIndex = remotePath.lastIndexOf('/');
        if(lastIndex > 0) {
            remoteDir = remoteDir.substring(0, lastIndex);
        }
        String remoteFile = lastIndex > 0 ? remotePath.substring(lastIndex + 1) : null;
        boolean isPattern = remoteFile != null && remoteFile.contains("*");

        if(!isPattern && remoteFile != null) {
            SftpClient.Attributes attributes = sftpClient.lstat(path);
            if(!attributes.isDirectory()) {
                log.info("Ssss" + Stream.of(new DirEntry(remoteFile, path, attributes)).toString());
            } else {
                remoteDir = remotePath;
            }
        }
        remoteDir =
            remoteDir.length() > 0 && remoteDir.charAt(0) == '/'
                ? remoteDir
                : sftpClient.canonicalPath(remoteDir);

        Stream<DirEntry> dirEntryStream = StreamSupport.stream(
                                                           sftpClient.readDir(remoteDir).spliterator(), false)
                                                       .filter(
                                                           (entry) -> !isPattern
                                                               || PatternMatchUtils.simpleMatch(
                                                               remoteFile,
                                                               entry.getFilename()));
        log.info("AsdfadsfasdfdsF" + dirEntryStream.toString());
    }

}
