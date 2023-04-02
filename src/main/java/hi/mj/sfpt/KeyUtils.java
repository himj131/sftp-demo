package hi.mj.sfpt;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

public class KeyUtils {

    public static PublicKey decodePublicKey(String key) throws Exception {
        InputStream stream = new ClassPathResource(key).getInputStream();
        byte[] keyBytes = FileCopyUtils.copyToByteArray(stream);
        // strip any newline chars
        while(keyBytes[keyBytes.length - 1] == 0x0a || keyBytes[keyBytes.length - 1] == 0x0d) {
            keyBytes = Arrays.copyOf(keyBytes, keyBytes.length - 1);
        }
        byte[] decodeBuffer = Base64.getDecoder().decode(keyBytes);
        ByteBuffer bb = ByteBuffer.wrap(decodeBuffer);
        int len = bb.getInt();
        byte[] type = new byte[len];
        bb.get(type);
        if("ssh-rsa".equals(new String(type))) {
            BigInteger e = decodeBigInt(bb);
            BigInteger m = decodeBigInt(bb);
            RSAPublicKeySpec spec = new RSAPublicKeySpec(m, e);
            return KeyFactory.getInstance("RSA").generatePublic(spec);

        } else {
            throw new IllegalArgumentException("Only supports RSA");
        }
    }

    private static BigInteger decodeBigInt(ByteBuffer bb) {
        int len = bb.getInt();
        byte[] bytes = new byte[len];
        bb.get(bytes);
        return new BigInteger(bytes);
    }
}
