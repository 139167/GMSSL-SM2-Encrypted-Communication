package test;

import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.util.encoders.Hex;

import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Scanner;

public class GmsslFuncTest {
    public static KeyPair createECKeyPair() {
        final ECGenParameterSpec sm2Spec = new ECGenParameterSpec("sm2p256v1");
        final KeyPairGenerator kpg;

        try {
            kpg = KeyPairGenerator.getInstance("EC", new BouncyCastleProvider());
            kpg.initialize(sm2Spec, new SecureRandom());
 
            return kpg.generateKeyPair();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
 
    public static String encrypt(String publicKeyHex, String data) {
        return encrypt(getECPublicKeyByPublicKeyHex(publicKeyHex), data, 1);
    }
 
    public static String encrypt(BCECPublicKey publicKey, String data, int modeType) {
        SM2Engine.Mode mode = SM2Engine.Mode.C1C3C2;
        if (modeType != 1) {
            mode = SM2Engine.Mode.C1C2C3;
        }
        ECParameterSpec ecParameterSpec = publicKey.getParameters();
        ECDomainParameters ecDomainParameters = new ECDomainParameters(ecParameterSpec.getCurve(),
                ecParameterSpec.getG(), ecParameterSpec.getN());
        ECPublicKeyParameters ecPublicKeyParameters = new ECPublicKeyParameters(publicKey.getQ(), ecDomainParameters);
 
        SM2Engine sm2Engine = new SM2Engine(mode);
 
        sm2Engine.init(true, new ParametersWithRandom(ecPublicKeyParameters, new SecureRandom()));
        byte[] arrayOfBytes = null;
        try {
            byte[] in = data.getBytes("utf-8");
        
            arrayOfBytes = sm2Engine.processBlock(in, 0, in.length);
        } catch (Exception e) {
            System.out.println("SM2加密时出现异常: " + e.getMessage());
            e.printStackTrace();
        }
        return Hex.toHexString(arrayOfBytes);
    }
 
    public static String decrypt(String privateKeyHex, String cipherData) {
        return decrypt(getBCECPrivateKeyByPrivateKeyHex(privateKeyHex), cipherData, 1);
    }
 
    public static String decrypt(BCECPrivateKey privateKey, String cipherData, int modeType) {
        SM2Engine.Mode mode = SM2Engine.Mode.C1C3C2;
        if (modeType != 1)
            mode = SM2Engine.Mode.C1C2C3;
       
        byte[] cipherDataByte = Hex.decode(cipherData);
        ECParameterSpec ecParameterSpec = privateKey.getParameters();
        ECDomainParameters ecDomainParameters = new ECDomainParameters(ecParameterSpec.getCurve(),
                ecParameterSpec.getG(), ecParameterSpec.getN());
        ECPrivateKeyParameters ecPrivateKeyParameters = new ECPrivateKeyParameters(privateKey.getD(),
                ecDomainParameters);
 
        SM2Engine sm2Engine = new SM2Engine(mode);
        sm2Engine.init(false, ecPrivateKeyParameters);
        String result = null;
        try {
            byte[] arrayOfBytes = sm2Engine.processBlock(cipherDataByte, 0, cipherDataByte.length);
            result = new String(arrayOfBytes, "utf-8");
        } catch (Exception e) {
            System.out.println("SM2解密时出现异常: " + e.getMessage());
        }
        return result;
    }
 
    private static X9ECParameters x9ECParameters = GMNamedCurves.getByName("sm2p256v1");
 
    private static ECParameterSpec ecDomainParameters = new ECParameterSpec(x9ECParameters.getCurve(), x9ECParameters.getG(), x9ECParameters.getN());
 
    public static BCECPublicKey getECPublicKeyByPublicKeyHex(String pubKeyHex) {
        if (pubKeyHex.length() > 128) {
            pubKeyHex = pubKeyHex.substring(pubKeyHex.length() - 128);
        }
        String stringX = pubKeyHex.substring(0, 64);
        String stringY = pubKeyHex.substring(64);
        BigInteger x = new BigInteger(stringX, 16);
        BigInteger y = new BigInteger(stringY, 16);
    
        ECPublicKeySpec ecPublicKeySpec = new ECPublicKeySpec(x9ECParameters.getCurve().createPoint(x, y), ecDomainParameters);
    
        return new BCECPublicKey("EC", ecPublicKeySpec, BouncyCastleProvider.CONFIGURATION);
    }
    
 
    public static BCECPrivateKey getBCECPrivateKeyByPrivateKeyHex(String privateKeyHex) {
        BigInteger d = new BigInteger(privateKeyHex, 16);
        ECPrivateKeySpec ecPrivateKeySpec = new ECPrivateKeySpec(d, ecDomainParameters);
        return new BCECPrivateKey("EC", ecPrivateKeySpec, BouncyCastleProvider.CONFIGURATION);
    }
 
    public static void main(String[] args) {
        String publicKeyHex = null;
        String privateKeyHex = null;
        KeyPair keyPair = createECKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        if (publicKey instanceof BCECPublicKey) {
            // Get 65-byte uncompressed public key (0x04)
            publicKeyHex = Hex.toHexString(((BCECPublicKey) publicKey).getQ().getEncoded(false));
            System.out.println("SM2 Public Key: " + publicKeyHex);
        }
        PrivateKey privateKey = keyPair.getPrivate();
        if (privateKey instanceof BCECPrivateKey) {
            // Get 32-byte private key
            privateKeyHex = ((BCECPrivateKey) privateKey).getD().toString(16);
            System.out.println("SM2 Private Key: " + privateKeyHex);
        }
 
        Scanner scanner = new Scanner(new InputStreamReader(System.in));
        System.out.print("\nInput Data Need Encrypt: ");
        String data = scanner.nextLine();

        String encryptData = encrypt(publicKeyHex, data);
        System.out.println("\nEncrypt Result: " + encryptData);

        data = decrypt(privateKeyHex, encryptData);
        System.out.println("\nDecrypt Result: " + data);
    }
}