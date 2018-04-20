package cn.fh.fabricsdk;

import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;

import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

public class Demo {
    public static void main(String[] args) throws Exception {
        // enroll("admin", "adminpw", "cert");
        queryCar();
        // updateCar();
        // queryCar();
    }


    /**
     * 更新账本
     * @throws Exception
     */
    private static void updateCar() throws Exception {
        HFClient client = HFClient.createNewInstance();
        Channel channel = initChannel(client);

        // 构建proposal
        TransactionProposalRequest req = client.newTransactionProposalRequest();
        // 指定要调用的chaincode
        ChaincodeID cid = ChaincodeID.newBuilder().setName("fabcar").build();
        req.setChaincodeID(cid);
        req.setFcn("changeCarOwner");
        req.setArgs("CAR1", "Bob");
        System.out.println("Executing for " + "CAR1");
        // 发送proprosal
        Collection<ProposalResponse> resps = channel.sendTransactionProposal(req);

        // 提交给orderer节点
        channel.sendTransaction(resps);
    }

    /**
     * 查询账本
     * @throws Exception
     */
    private static void queryCar() throws Exception {
        HFClient client = HFClient.createNewInstance();
        Channel channel = initChannel(client);

        String key = "name";

        // 构建proposal
        QueryByChaincodeRequest req = client.newQueryProposalRequest();
        // 指定要调用的chaincode
        ChaincodeID cid = ChaincodeID.newBuilder().setName("cert").build();
        req.setChaincodeID(cid);
        req.setFcn("queryId");
        req.setArgs(key);
        System.out.println("Querying for " + key);
        Collection<ProposalResponse> resps = channel.queryByChaincode(req);

        for (ProposalResponse resp : resps) {
            String payload = new String(resp.getChaincodeActionResponsePayload());
            System.out.println("response: " + payload);
        }
    }

    /**
     * 用户注册, 保存证书和私钥
     *
     * @param username Fabric CA Admin用户的用户名
     * @param password Fabric CA Admin用户的密码
     * @param certDir 目录名, 用来保存证书和私钥
     * @throws Exception
     */
    private static void enroll(String username, String password, String certDir) throws Exception {
        HFClient client = HFClient.createNewInstance();
        CryptoSuite cs = CryptoSuite.Factory.getCryptoSuite();
        client.setCryptoSuite(cs);

        Properties prop = new Properties();
        prop.put("verify", false);
        HFCAClient caClient = HFCAClient.createNewInstance("http://127.0.0.1:7054", prop);
        caClient.setCryptoSuite(cs);


        // enrollment保存了证书和私钥
        Enrollment enrollment = caClient.enroll(username, password);
        System.out.println(enrollment.getCert());

        // 保存到本地文件
        CertUtils.saveEnrollment(enrollment, certDir, username);
    }

    private static Channel initChannel(HFClient client) throws Exception {
        CryptoSuite cs = CryptoSuite.Factory.getCryptoSuite();
        client.setCryptoSuite(cs);

        client.setUserContext(
                new CarUser(
                        "admin",
                        CertUtils.loadEnrollment("cert", "admin")
                )
        );

        // 初始化channel
        Channel channel = client.newChannel("mychannel");
        channel.addPeer(client.newPeer("peer", "grpc://127.0.0.1:7051"));
        // 指定排序节点地址, 无论是后面执行查询还是更新都必须指定排序节点
        channel.addOrderer(client.newOrderer("orderer", "grpc://127.0.0.1:7050"));
        channel.initialize();

        return channel;
    }
}

/**
 * User接口实现类
 */
class CarUser implements User {
    private String name;
    private Enrollment enrollment;

    public CarUser(String name, Enrollment enrollment) {
        this.name = name;
        this.enrollment = enrollment;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Set<String> getRoles() {
        return Collections.emptySet();
    }

    @Override
    public String getAccount() {
        return "";
    }

    @Override
    public String getAffiliation() {
        return "";
    }

    @Override
    public Enrollment getEnrollment() {
        return this.enrollment;
    }

    @Override
    public String getMspId() {
        return "Org1MSP";
    }
}
