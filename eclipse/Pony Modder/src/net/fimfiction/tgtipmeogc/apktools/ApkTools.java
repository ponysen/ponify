package net.fimfiction.tgtipmeogc.apktools;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import kellinwood.security.zipsigner.ZipSigner;

import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.x509.X509V3CertificateGenerator;

public class ApkTools {
	
	private byte[] mOrigBytes;
	private JarOutputStream mOutput;
	private ByteArrayInputStream mInput;
	private Set<String> mIgnore;
	
	public ApkTools(InputStream input, OutputStream output) throws IOException {
		mIgnore = new HashSet<String>();
		
		ByteArrayOutputStream origBytes = new ByteArrayOutputStream();
		byte buffer[] = new byte[4048];
		int bufferLength;
		
		while((bufferLength = input.read(buffer)) > 0) {
			origBytes.write(buffer, 0, bufferLength);
		}
		
		mOrigBytes = origBytes.toByteArray();
		mInput = new ByteArrayInputStream(mOrigBytes);
		
		mOutput = new JarOutputStream(output);
		mOutput.setLevel(9);
		
	}
	
	public byte[] getEntry(String path) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		JarInputStream input = new JarInputStream(mInput);
		
		for(JarEntry entry = input.getNextJarEntry(); entry != null; entry = input.getNextJarEntry()) {
			String entryName = entry.getName();
			
			if(!path.equals(entryName)) {
				continue;
			}
			
				
			byte[] block = new byte[4048];
			int length;
				
			while((length = input.read(block)) > 0) {
				output.write(block, 0, length);
			}
				
		}
		
		mInput.reset();
		
		return output.toByteArray();
	}
	
	public void place() throws IOException {
		JarInputStream input = new JarInputStream(mInput, false);
		
		for(JarEntry entry = input.getNextJarEntry(); entry != null; entry = input.getNextJarEntry()) {
			String entryName = entry.getName();
			
			if(mIgnore.contains(entryName)) {
				continue;
			}
			
				
			if(!entryName.startsWith("META-INF/")){
				ZipEntry newEntry = new ZipEntry(entry.getName());
				
				newEntry.setMethod(entry.getMethod());
				
				if(newEntry.getMethod() == ZipEntry.STORED) {
					newEntry.setSize(entry.getSize());
					newEntry.setCrc(entry.getCrc());
				}
				
				mOutput.putNextEntry(newEntry);
				
				byte[] block = new byte[4048];
				int length;
				
				while((length = input.read(block)) > 0) {
					mOutput.write(block, 0, length);
				}
				
				mOutput.closeEntry();
			}
			
			mOutput.flush();
		}
		
		mInput.reset();
		mOutput.close();
		
	}
	
	public void replace(String filename, byte[] source) throws IOException {
		JarEntry newEntry = new JarEntry(filename);
		mOutput.putNextEntry(newEntry);
		mOutput.write(source);
		mOutput.closeEntry();
		
		mIgnore.add(filename);
	}
	
	/**
	 * Signs an APK file with a randomly generated key.
	 * 
	 * @param unsignedApk
	 * @param outputFileName
	 */
	public static void signApk(String unsignedApk, String outputFileName) {
		try {
			ZipSigner apkSigner = new ZipSigner();
			
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(1024);
			KeyPair keyPair = keyGen.generateKeyPair();
			
			PrivateKey privateKey = keyPair.getPrivate();
			X509Certificate publicKey = generateCertificate(keyPair);
			apkSigner.setKeys("ZE ZEKRIT KEY", publicKey, privateKey, null);
			apkSigner.signZip(unsignedApk, outputFileName);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static X509Certificate generateCertificate(KeyPair keyPair) {
		X509Principal issuer = new X509Principal("C=C, ST=ST, L=L, O=O, OU=OU, CN=CN");
		BigInteger serial = new BigInteger("" + System.currentTimeMillis());
		Date notBefore = Calendar.getInstance().getTime();
		X509Name subject = new X509Name("C=C, ST=ST, L=L, O=O, OU=OU, CN=CN");
		
		Calendar endDate = Calendar.getInstance();
		endDate.add(Calendar.YEAR, 30);
		Date notAfter = endDate.getTime();
		
		
		X509V3CertificateGenerator generator = new X509V3CertificateGenerator();
		generator.setIssuerDN(issuer);
		generator.setSerialNumber(serial);
		generator.setNotBefore(notBefore);
		generator.setNotAfter(notAfter);
		generator.setSubjectDN(subject);
		generator.setSignatureAlgorithm("SHA1WITHRSA");
		generator.setPublicKey(keyPair.getPublic());
		
		try {
			return generator.generate(keyPair.getPrivate());
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	

}
