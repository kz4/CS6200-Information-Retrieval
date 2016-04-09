package com.ir.readtrec07p;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.print.Doc;

import com.ir.util.Constants;
import com.ir.util.FileWriter_Helper;

public class ReadTrec07P {

	public Map<Integer, String> mailId_label_;
	
	public ReadTrec07P(){
		mailId_label_ = new HashMap<Integer, String>();
	}
	
	public void read_spam_ham(String filename){

		int mailId = 1;
		try {
			FileReader fr = new FileReader(filename);
			BufferedReader br = new BufferedReader(fr);

			String line = null;
			while ((line = br.readLine()) != null) {
				String[] spamOrHam_mail = line.split(" ..");
				String spamOrHam = spamOrHam_mail[0];
				mailId_label_.put(mailId, spamOrHam);
				mailId++;
			}

			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public List<DocumentInfo> read_trec_mails(String filename){
		
		int totalNumFiles = new File(filename).listFiles().length;
		
		// Minus 1 because first file is DS_Store. Total: 75419
		totalNumFiles = totalNumFiles - 1;
		
		List<DocumentInfo> lstDocInfo = new ArrayList<DocumentInfo>();
		
		for (int mailId = 1; mailId <= totalNumFiles; mailId++) {
			try {
				String mailFilename = filename + "inmail." + mailId;
				String shortMailfileName = "inmail." + mailId;
				
				// Deal with mailId = 7
				Charset charset = Charset.forName("ISO-8859-1");
				String wholeEmail = String.join(Constants.newline, Files.readAllLines(Paths.get(mailFilename), charset));
				
				// Split all emails into 2 halves. The first half is the header information, and the second half is the email content 
				String[] headers_body = wholeEmail.split(Constants.newline + Constants.newline, 2);
				String headers = headers_body[0];
				String htmlContent = "";
				
				// Have to deal with this because for mailId 25769, there is
				// no content in this email
				if (headers_body.length == 2){
					htmlContent = headers_body[1];
				}
				StringBuilder sb = new StringBuilder();
				
				String subject = getSubject(headers);
		        sb.append(subject);
				sb.append(Constants.newline);

				if (headers_body.length == 2){
			        String content = getContent(htmlContent);
			        sb.append(content);
					sb.append(Constants.newline);
				}
				
		        String body = sb.toString();
		        
		        // Replace multiple spaces with 1
		        // body = body.replace("\\s+", " ");
		        
		        // Replace multiple new lines with 1, to deal with case, e.g. inmail.1716, inmail.1723
		        // body = body.replace("[\r\n]+", Constants.newline);
		        
		        // Replace multiple spaces, new lines and a tab with one space
		        body = body.replaceAll("^\\s+|\\s+$|\\s*(\n)\\s*|(\\s)\\s*", " ")
		        .replace("\t"," ");
		        
		        DocumentInfo doc = new DocumentInfo();
		        doc.file_name_ = shortMailfileName;
		        doc.body_ = body;
		        doc.label_ = mailId_label_.get(mailId);
		        doc.split_ = randomSplitTrainTest();
		        lstDocInfo.add(doc);
				
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
		
		return lstDocInfo;
	}

	/**
	 * Randomly split data into TRAIN 80% and TEST 20%
	 * If the random int = 1, we return TEST
	 * Otherwise, we return TRAIN
	 * @return
	 */
	private String randomSplitTrainTest() {
		int min = 1;
		int max = 5;
		
		Random random = new Random();
		int rand = random.nextInt(max - min + 1) + min;
		if (rand == min){
			return "test";
		} else {
			return "train";
		}
	}

	private String getSubject(String headers) {
		StringBuilder sb = new StringBuilder();
		String[] lines = headers.split(Constants.newline);
		
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].startsWith("Subject: ")){
				sb.append(lines[i]);
				sb.append(Constants.newline);
			}
		}
		return sb.toString();
	}
	
	private String getContent(String htmlContent) {
		String stripped = htmlContent.replaceAll("<[^>]*>", "");
		
		StringBuilder sb = new StringBuilder();
		String[] lines = stripped.split(Constants.newline);
		Set<String> skipped_set = get_skipped_content();
		for (String line : lines) {
			if (skipped_set.parallelStream().anyMatch(skippingPart -> line.startsWith(skippingPart))
					|| is_encoded_line(line))
				continue;
			
			if (line.startsWith("> "))
				line.replace("> ", "");
			
			if (line.equals(""))
				continue;
			
			sb.append(line);
			sb.append(Constants.newline);
		}

		return sb.toString();
	}

	private Set<String> get_skipped_content() {
		Set<String> skipped_set = new HashSet<String>();
		skipped_set.add("------=_NextPart");
		skipped_set.add("Content-Type: ");
		skipped_set.add("	charset=");
		skipped_set.add("        boundary=");
		skipped_set.add("Content-Transfer-Encoding: ");
		skipped_set.add("----");
		skipped_set.add("        charset=");
		skipped_set.add("This is a multi-part message in MIME format.");
		skipped_set.add("Content-ID: ");
		return skipped_set;
	}
	
	private Boolean is_encoded_line(String line){
		String[] splittedLine = line.split("\\s+");
		if (splittedLine.length == 1 && splittedLine[0].length() > 40)
			return true;
		else
			return false;
	}

	public static void main(String[] args) {

		System.out.println("Started reading mailId label map");
		ReadTrec07P trec = new ReadTrec07P();
		trec.read_spam_ham(Constants.spamHamInfo);
		System.out.println("Finished reading mailId label map");
		
		System.out.println("Started populating mailId body label split list");
		List<DocumentInfo> lstDocInfo = trec.read_trec_mails(Constants.dataFol);
		
		for (DocumentInfo documentInfo : lstDocInfo) {
			String info = documentInfo.file_name_ + " " + documentInfo.label_ + " " + documentInfo.split_+ " " + documentInfo.body_;
			FileWriter_Helper.appendToFile(Constants.lstDocInfo, info);
		}
		
		System.out.println("Finished populating mailId body label split list");
	}

}
