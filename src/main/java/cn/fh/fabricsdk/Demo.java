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
        // queryCar();
        // updateCar();
        // queryCar();
    }


    private static void updateCar() throws Exception {
        HFClient client = HFClient.createNewInstance();
        Channel channel = initChannel(client);

        TransactionProposalRequest req = client.newTransactionProposalRequest();
        ChaincodeID cid = ChaincodeID.newBuilder().setName("fabcar").build();
        req.setChaincodeID(cid);
        req.setFcn("changeCarOwner");
        req.setArgs(new String[] { "CAR1", "Bob" });
        System.out.println("Executing for " + "CAR1");
        Collection<ProposalResponse> resps = channel.sendTransactionProposal(req);

        channel.sendTransaction(resps);
    }

    private static void queryCar() throws Exception {
        HFClient client = HFClient.createNewInstance();
        Channel channel = initChannel(client);

        String key = "CAR1";
        QueryByChaincodeRequest req = client.newQueryProposalRequest();
        ChaincodeID cid = ChaincodeID.newBuilder().setName("fabcar").build();
        req.setChaincodeID(cid);
        req.setFcn("queryCar");
        req.setArgs(new String[] { key });
        System.out.println("Querying for " + key);
        Collection<ProposalResponse> resps = channel.queryByChaincode(req);

        for (ProposalResponse resp : resps) {
            String payload = new String(resp.getChaincodeActionResponsePayload());
            System.out.println("response: " + payload);
        }
    }

    private static void enroll(String username, String password, String certDir) throws Exception {
        HFClient client = HFClient.createNewInstance();
        CryptoSuite cs = CryptoSuite.Factory.getCryptoSuite();
        client.setCryptoSuite(cs);

        Properties prop = new Properties();
        prop.put("verify", false);
        HFCAClient caClient = HFCAClient.createNewInstance("http://127.0.0.1:7054", prop);
        caClient.setCryptoSuite(cs);


        Enrollment enrollment = caClient.enroll(username, password);
        System.out.println(enrollment.getCert());

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

        // Instantiate channel
        Channel channel = client.newChannel("mychannel");
        channel.addPeer(client.newPeer("peer", "grpc://127.0.0.1:7051"));
        // It always wants orderer, otherwise even query does not work
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
