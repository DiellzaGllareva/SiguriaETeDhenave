package Kodet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Random;
import java.util.Scanner;

public class WriteRead {
	private String name;

	public WriteRead(String name) {
		this.name = name;
	}

	public PublicKey getPublicElements(String namee) throws Exception {
		try {
			File file = new File("./keys/" + namee + ".pub.xml");
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();

			NodeList nodeList = doc.getElementsByTagName("RSAKeyValue");
			Node node = nodeList.item(0);
			Element eElement = (Element) node;
			String modulus = eElement.getElementsByTagName("Modulus").item(0).getTextContent();
			String exponent = eElement.getElementsByTagName("Exponent").item(0).getTextContent();
			KeyFactory rsaFactory = KeyFactory.getInstance("RSA");

			byte[] modBytes = modulus.getBytes();
			byte[] expBytes = exponent.getBytes();
			BigInteger modBigInt = new BigInteger(1, Base64.getDecoder().decode(modBytes));
			BigInteger expBigInt = new BigInteger(1, Base64.getDecoder().decode(expBytes));

			RSAPublicKeySpec rsaKeyspec;
			rsaKeyspec = new RSAPublicKeySpec(modBigInt, expBigInt);
			PublicKey key = rsaFactory.generatePublic(rsaKeyspec);
			return key;
		} catch (FileNotFoundException e) {
			System.out.println("Gabim: Celesi publik '" + namee + "' nuk ekziston.");
			System.exit(1);
		}
		return null;
	}

	public PrivateKey getPrivateElements(String user) throws Exception {
		try {
			File file = new File("./keys/" + user + ".xml");
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();

			NodeList nodeList = doc.getElementsByTagName("RSAKeyValue");
			Node node = nodeList.item(0);

			Element eElement = (Element) node;
			String modulus = eElement.getElementsByTagName("Modulus").item(0).getTextContent();
			String d = eElement.getElementsByTagName("D").item(0).getTextContent();
			KeyFactory rsaFactory = KeyFactory.getInstance("RSA");

			byte[] modBytes = modulus.getBytes();
			byte[] dBytes = d.getBytes();
			BigInteger modBigInt = new BigInteger(1, Base64.getDecoder().decode(modBytes));
			BigInteger dBigInt = new BigInteger(1, Base64.getDecoder().decode(dBytes));

			RSAPrivateKeySpec rsaKeyspec = new RSAPrivateKeySpec(modBigInt, dBigInt);
			PrivateKey key = rsaFactory.generatePrivate(rsaKeyspec);

			return key;
		} catch (FileNotFoundException e) {
			System.out.println("Gabim: Celesi privat '" + user + "' nuk ekziston.");
			System.exit(1);
		}
		return null;
	}

	public static String sign(byte[] plainText, PrivateKey privateKey) throws Exception {
		Signature privateSignature = Signature.getInstance("SHA256withRSA");
		// byte[] signature = privateSignature.sign();
		privateSignature.initSign(privateKey);
		privateSignature.update(plainText);
		return Base64.getEncoder().encodeToString(privateSignature.sign());
	}

	public String encrypt(String plainText) throws Exception {
		PublicKey publicKey = getPublicElements(name);
		Cipher encryptCipher = Cipher.getInstance("RSA");
		encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
		byte[] cipherbytes = encryptCipher.doFinal(plainText.getBytes());
		String cipherText = Base64.getEncoder().encodeToString(cipherbytes);
		return cipherText;
	}

	public String decrypt(String data, String user) throws Exception {
		PrivateKey privateKey = getPrivateElements(user);
		Cipher decriptCipher = Cipher.getInstance("RSA");
		decriptCipher.init(Cipher.DECRYPT_MODE, privateKey);
		byte[] bytes = Base64.getDecoder().decode(data.getBytes());
		return new String(decriptCipher.doFinal(bytes));
	}

	public IvParameterSpec generateIV() {
		byte[] iv = new byte[8];
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(iv);
		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
		return ivParameterSpec;
	}

	public SecretKeySpec generateDESKey() {
		Random random = new Random();
		byte[] keyBytes = new byte[8];
		random.nextBytes(keyBytes);
		SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "DES");
		return secretKey;
	}

	public String encryptTextDES(String plaintext, String IV, String deskey)
			throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
			InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
		Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
		byte[] keyBytes = Base64.getDecoder().decode(deskey.getBytes());
		byte[] ivBytes = Base64.getDecoder().decode(IV.getBytes());
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "DES"), new IvParameterSpec(ivBytes));
		return Base64.getEncoder().encodeToString(cipher.doFinal(plaintext.getBytes()));
	}

	public String decryptTextDES(String ciphertext, String IV, String deskey)
			throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
			InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
		Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
		byte[] keyBytes = Base64.getDecoder().decode(deskey.getBytes());
		byte[] ivBytes = Base64.getDecoder().decode(IV.getBytes());
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "DES"), new IvParameterSpec(ivBytes));
		byte[] bytes = Base64.getMimeDecoder().decode(ciphertext.getBytes());
		return new String(cipher.doFinal(bytes));
	}

	public String encryptWrite(String plaintext) throws Exception {
		String IV = Base64.getEncoder().encodeToString(generateIV().getIV());
		String deskey = Base64.getEncoder().encodeToString(generateDESKey().getEncoded());
		String uname = Base64.getEncoder().encodeToString(name.getBytes());
		return uname + "." + IV + "." + encrypt(deskey) + "." + encryptTextDES(plaintext, IV, deskey);

	}

	private static String decode64(String str) {
		Base64.Decoder decoder = Base64.getDecoder();
		byte[] dbyte = decoder.decode(str.getBytes());
		return new String(dbyte);
	}

	public String encryptWrite1(String plaintext, String token) throws Exception {
		String IV = Base64.getEncoder().encodeToString(generateIV().getIV());
		String deskey = Base64.getEncoder().encodeToString(generateDESKey().getEncoded());
		String uname = Base64.getEncoder().encodeToString(name.getBytes());
		String[] pikat = token.split("\\.");
		String senderr = pikat[0];
		String sender = decode64(pikat[0]);
		String y = encryptTextDES(plaintext, IV, deskey);
		byte[] x = Base64.getDecoder().decode(y.getBytes());
		// String sig= x.toString();
		PrivateKey aKey = getPrivateElements(sender);
		String q = sign(x, aKey);
		System.out.println("Rezultati: ");
		return uname + "." + IV + "." + encrypt(deskey) + "." + encryptTextDES(plaintext, IV, deskey) + "." + senderr
				+ "." + q;
	}

	public String decryptRead(String ciphertext) throws Exception {
		String[] textArray = ciphertext.split("\\.");
		String user = new String(Base64.getDecoder().decode(textArray[0].getBytes()));
		String deskey = decrypt(textArray[2], user);
		StringBuilder sb = new StringBuilder();
		sb.append("Pranuesi: ");
		sb.append(user);
		sb.append("\n");
		sb.append("Mesazhi: ");
		sb.append(decryptTextDES(textArray[3], textArray[1], deskey));
		return sb.toString();
	}

	public void decryptRead1(String signature) throws Exception {
		String[] textArray = signature.split("\\.");
		String user = new String(Base64.getDecoder().decode(textArray[0].getBytes()));
		System.out.println("Marresi: " + user);
		String deskey = decrypt(textArray[2], user);
		System.out.println("Mesazhi: " + decryptTextDES(textArray[3], textArray[1], deskey));
		String derguesi = new String(Base64.getDecoder().decode(textArray[4].getBytes()));
		System.out.println("Derguesi: " + derguesi);
		String y = decryptTextDES(textArray[3], textArray[1], deskey);
		byte[] plaintext = Base64.getDecoder().decode(textArray[3].getBytes());
		PublicKey aKey = getPublicElements(derguesi);
		String sig = textArray[5];
		boolean verify = verify(plaintext, sig, aKey);
		if (verify == true) {
			System.out.println("Nenshkrimi: Valid");
		} else {
			System.out.println("Nenshkrimi: Jovalid. Celesi mungon ose nenshkrimi eshte jo valid!");
		}

	}

	public static boolean verify(byte[] plainText, String signature, PublicKey publicKey) throws Exception {
		byte[] signatureBytes = Base64.getMimeDecoder().decode(signature);
		Signature publicSignature = Signature.getInstance("SHA256withRSA");
		publicSignature.initVerify(publicKey);
		publicSignature.update(plainText);
		return publicSignature.verify(signatureBytes);
	}

	public void Write(String plaintext, String filePath, String sender) throws Exception {
		if (filePath == null && sender == null) {
			System.out.println(encryptWrite(plaintext));
		} else if (filePath == null) {
			System.out.println(encryptWrite1(plaintext, sender));
		} else {
			String text = encryptWrite(plaintext);
			writeFile(text, filePath);
			System.out.println("Mesazhi i enkriptuar u ruajt ne fajllin " + filePath);
		}

	}

	public void Read(String ciphertext) throws Exception {
		File file = new File(ciphertext);
		String[] textArray = ciphertext.split("\\.");
		int size = textArray.length;
		if (file.exists()) {
			String text = readFile(ciphertext);
			System.out.println(decryptRead(text));
		} else if (size == 6) {
			decryptRead1(ciphertext);
		} else {
			System.out.println(decryptRead(ciphertext));
		}
	}

	public void writeFile(String text, String filename) throws Exception {
		try (PrintWriter writer = new PrintWriter(filename)) {
			writer.write(text);
		}
	}

	public String readFile(String fileName) throws FileNotFoundException {
		StringBuilder sb = new StringBuilder();
		try {
			File file = new File(fileName);
			if ((file.exists())) {
				Scanner reader = new Scanner(file);
				while (reader.hasNextLine()) {
					sb.append(reader.nextLine());
					sb.append("\n");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
}
