package cn.fh.fabricsdk;

import org.hyperledger.fabric.sdk.Enrollment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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

        String certFileName = String.join("", dir, File.separator, name, ".cert");
        try (FileOutputStream certOut = new FileOutputStream(certFileName)) {
            certOut.write(enrollment.getCert().getBytes());

        } catch (IOException ex) {
            throw ex;
        }

        String keyFileName = String.join("", dir, File.separator, name, ".priv");
        try (FileOutputStream keyOut = new FileOutputStream(keyFileName)) {
            keyOut.write(enrollment.getKey().getEncoded());

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
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

        byte[] certBuf = Files.readAllBytes(Paths.get(dir, name + ".cert"));
        String cert = new String(certBuf);

        byte[] keyBuf = Files.readAllBytes(Paths.get(dir, name + ".priv"));
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBuf);
        KeyFactory kf = KeyFactory.getInstance("EC");
        PrivateKey key = kf.generatePrivate(keySpec);

        return new MyEnrollment(key, cert);
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
