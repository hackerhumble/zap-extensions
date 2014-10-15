/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2012 The ZAP development team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.zap.extension.pscanrulesAlpha;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.htmlparser.jericho.Source;

import org.parosproxy.paros.Constant;
import org.parosproxy.paros.core.scanner.Alert;
import org.parosproxy.paros.network.HttpMessage;
import org.zaproxy.zap.extension.pscan.PassiveScanThread;
import org.zaproxy.zap.extension.pscan.PluginPassiveScanner;


/**
 * A class to passively scan response for signatures that are indicative that Directory Browsing / Listing is enabled
 * @author 70pointer@gmail.com
 *
 */
public class DirectoryBrowsingScanner extends PluginPassiveScanner {

	private PassiveScanThread parent = null;
	
	/**
	 * a consistently ordered map of: a regular expression pattern, mapping to the Web server to which the pattern most likely corresponds
	 */
	static Map <Pattern, String> serverPatterns = new LinkedHashMap <Pattern, String> ();
	
	static {
		//Apache 2
		serverPatterns.put(Pattern.compile("<title>Index of /[^<]+?</title>", Pattern.MULTILINE | Pattern.DOTALL), "Apache 2");
		//TODO: add patterns here for other web servers, once these are available
		}

	/**
	 * Prefix for internationalized messages used by this rule
	 */
	private static final String MESSAGE_PREFIX = "pscanalpha.directorybrowsing.";

	/**
	 * gets the name of the scanner
	 * @return
	 */
	@Override
	public String getName() {
		return Constant.messages.getString(MESSAGE_PREFIX + "name");
	}

	/**
	 * scans the HTTP request sent (in fact, does nothing)
	 * @param msg
	 * @param id
	 */
	@Override
	public void scanHttpRequestSend(HttpMessage msg, int id) {
		// do nothing
	}

	/**
	 * scans the HTTP response for signatures that might indicate Directory Browsing 
	 * @param msg
	 * @param id
	 * @param source unused
	 */
	@Override
	public void scanHttpResponseReceive(HttpMessage msg, int id, Source source) {
		//get the body contents as a String, so we can match against it
		String responsebody = new String (msg.getResponseBody().getBytes());
		
		//try each of the patterns in turn against the response.
		String evidence = null;
		String server = null;
		Iterator<Pattern> patternIterator = serverPatterns.keySet().iterator();
		while (patternIterator.hasNext()) {
			Pattern serverPattern = patternIterator.next();
			server = serverPatterns.get(serverPattern);
			Matcher matcher = serverPattern.matcher(responsebody);
	        if (matcher.find()) {
	            evidence = matcher.group();
	            break;	//use the first match
	        }	    
		}
		if (evidence!=null && evidence.length() > 0) {
			//we found something
			Alert alert = new Alert(getPluginId(), Alert.RISK_MEDIUM, Alert.WARNING, getName() + " - "+ server );		
			     
			alert.setDetail(
					getDescription() + " - "+ server, 
					msg.getRequestHeader().getURI().toString(), 
					"", //param
					"", //attack 
					getExtraInfo(msg, evidence),  //other info
					getSolution(), 
					getReference(), 
					evidence,	
					548,	//Information Exposure Through Directory Listing
					16,		//Directory Indexing
					msg);  
			parent.raiseAlert(id, alert);
		}
		
	}

	/**
	 * sets the parent
	 * @param parent
	 */
	@Override
	public void setParent(PassiveScanThread parent) {
		this.parent = parent;
	}

	/**
	 * get the id of the scanner
	 * @return
	 */
	@Override
	public int getPluginId() {
		return 10033;
	}

	/**
	 * get the description of the alert
	 * @return
	 */
	private String getDescription() {
		return Constant.messages.getString(MESSAGE_PREFIX + "desc");
	}

	/**
	 * get the solution for the alert
	 * @return
	 */
	private String getSolution() {
		return Constant.messages.getString(MESSAGE_PREFIX + "soln");
	}

	/**
	 * gets references for the alert
	 * @return
	 */
	private String getReference() {
		return Constant.messages.getString(MESSAGE_PREFIX + "refs");
	}

	/**
	 * gets extra information associated with the alert
	 * @param msg
	 * @param arg0
	 * @return
	 */
	private String getExtraInfo(HttpMessage msg, String arg0) {		
		return Constant.messages.getString(MESSAGE_PREFIX + "extrainfo", arg0);        
	}

}

