package cn.fh.fabricsdk;

import org.hyperledger.fabric.sdk.Enrollment;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class CertUtils {
    private CertUtils() {}

    /**
     * 保存cert, key到文件
     * @param enrollment Client返回的Enrollment对象
     * @param dir 要保存的目录
     * @param name 用户名
     * @throws IOException
     */
    public static void saveEnrollment(Enrollment enrollment, String dir, String name) throws IOException {
        if (null == enrollment) {
            throw new IllegalStateException("enrollment cannot be null");
        }

        // 保存cert
        String certFileName = String.join("", dir, File.separator, name, ".cert");
        try (FileOutputStream certOut = new FileOutputStream(certFileName)) {
            certOut.write(enrollment.getCert().getBytes());

        } catch (IOException ex) {
            throw ex;
        }

        // 保存private key
        String keyFileName = String.join("", dir, File.separator, name, ".priv");
        try (FileOutputStream keyOut = new FileOutputStream(keyFileName)) {
            StringBuilder sb = new StringBuilder(300);
            sb.append("-----BEGIN PRIVATE KEY-----\n");

            String priKey = DatatypeConverter.printBase64Binary(enrollment.getKey().getEncoded());
            // 每64个字符输出一个换行
            int LEN = priKey.length();
            for (int ix = 0; ix < LEN; ++ix) {
                sb.append(priKey.charAt(ix));

                if ((ix + 1) % 64 == 0) {
                    sb.append('\n');
                }
            }

            sb.append("\n-----END PRIVATE KEY-----\n");
            keyOut.write(sb.toString().getBytes());

        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 从文件中读取身份信息
     * @param dir
     * @param name
     * @return
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static Enrollment loadEnrollment(String dir, String name)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, GeneralSecurityException {

        byte[] certBuf = Files.readAllBytes(Paths.get(dir, name + ".cert"));
        String cert = new String(certBuf);

        // 读取文件, 构造PrivateKey对象
        PrivateKey key = loadPrivateKey(Paths.get(dir, name + ".priv"));

        return new MyEnrollment(key, cert);
    }

    /***
     * loading private key from .pem-formatted file, ECDSA algorithm
     * (from some example on StackOverflow, slightly changed)
     * @param fileName - file with the key
     * @return Private Key usable
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private static PrivateKey loadPrivateKey(Path fileName) throws IOException, GeneralSecurityException {
        PrivateKey key = null;
        InputStream is = null;
        try {
            is = new FileInputStream(fileName.toString());
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();
            boolean inKey = false;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (!inKey) {
                    if (line.startsWith("-----BEGIN ") && line.endsWith(" PRIVATE KEY-----")) {
                        inKey = true;
                    }
                    continue;
                } else {
                    if (line.startsWith("-----END ") && line.endsWith(" PRIVATE KEY-----")) {
                        inKey = false;
                        break;
                    }
                    builder.append(line);
                }
            }
            //
            byte[] encoded = DatatypeConverter.parseBase64Binary(builder.toString());
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            // KeyFactory kf = KeyFactory.getInstance("ECDSA");
            KeyFactory kf = KeyFactory.getInstance("EC");
            key = kf.generatePrivate(keySpec);
        } finally {
            is.close();
        }
        return key;
    }

}

class MyEnrollment implements Enrollment {
    private PrivateKey privateKey;
    private String cert;

    public MyEnrollment(PrivateKey privateKey, String cert) {
        this.privateKey = privateKey;
        this.cert = cert;
    }

    @Override
    public PrivateKey getKey() {
        return this.privateKey;
    }

    @Override
    public String getCert() {
        return this.cert;
    }
}
