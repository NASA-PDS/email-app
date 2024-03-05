// Copyright 2016-2017, by the California Institute of Technology.
// ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
// Any commercial use must be negotiated with the Office of Technology Transfer
// at the California Institute of Technology.
//
// This software is subject to U. S. export control laws and regulations
// (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the extent that the software
// is subject to U.S. export control laws and regulations, the recipient has
// the responsibility to obtain export licenses or other export authority as
// may be required before exporting such information to foreign countries or
// providing access to foreign nationals.
//
// $Id: $

package gov.nasa.pds.email;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet that takes message details from user and send it as a new e-mail
 * through an SMTP server.
 * 
 * @author www.codejava.net
 * 
 */
public class EmailSendingServlet extends HttpServlet {

  private static Logger LOG = Logger.getLogger(EmailSendingServlet.class.getName());

	private static final long serialVersionUID = 5058857889035951692L;
	private String host;
	private String port;
	private String user;
	private String pass;
	private String to;
	private int maxMsgNums = 200;
	final static String encType = "UTF-8";

	public void init() {
		// reads SMTP server setting from web.xml file
		ServletContext context = getServletContext();
		host = context.getInitParameter("host");
		port = context.getInitParameter("port");
		user = context.getInitParameter("user");
		pass = context.getInitParameter("pass");
		to = context.getInitParameter("recipients");
		maxMsgNums = Integer.parseInt(context.getInitParameter("max_msg_nums"));
	}

	static String extractPostRequestBody(HttpServletRequest request) throws IOException {
		Scanner s = null;
		try {
		if ("POST".equalsIgnoreCase(request.getMethod())) {
			s = new Scanner(request.getInputStream(), encType).useDelimiter("\\A");
			return s.hasNext() ? s.next() : "";
		}
		return "";
		} finally {
			if (s != null) {
				s.close();
			}
		}
	}

	public void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		response.addHeader("Access-Control-Allow-Origin", "*");
		String subject = "", msg = "";

		String reqBody = extractPostRequestBody(request);
        LOG.info("Email App Request: " + reqBody);

		String[] items = reqBody.split("&");
		for (int i=0; i<items.length; i++) {
			if (items[i].startsWith("subject")) {
				subject = items[i].substring(items[i].indexOf("=")+1);
				subject = URLDecoder.decode(subject, encType);
			}
			if (items[i].startsWith("content")) {
				msg = items[i].substring(items[i].indexOf("=")+1);
				msg = URLDecoder.decode(msg, encType); 
			}
		}

		String resultMessage = "";
		try {
			SendEmail mailer;
			
			if (user.equals("") && pass.equals("")) {
				mailer = new SendEmail(host, port);
                LOG.fine("set mailer with host, port...");
			} 
			else {
				if (user!=null && pass!=null) {
					mailer = new SendEmail(host, port, user, pass);
                    LOG.fine("set mailer with host, port, user, pass...");
				}
				else { 
					mailer = new SendEmail(host, port);
                    LOG.fine("set mailer with host, port...");
				}
			}
			mailer.setMaxMsgNums(maxMsgNums);

			if (mailer!=null) {
				ArrayList<String> addressList;
				if (to.contains(",") || to.contains("%2C")) {
					if (to.contains("%2C")) {
						to = to.replace("%2C",",");
					}
					addressList = new ArrayList<String>(Arrays.asList(to.split(",")));
					mailer.send(addressList, subject, msg);
                    LOG.info("mailing to multiple users: " + to);
				}
				else {
					mailer.send(to, subject, msg);  
                    LOG.info("mailing to one user: " + to);
				}

				resultMessage = "The e-mail was sent successfully";			
				//resultMessage += "\nTO: " + to
				//		      + "\nSUBJECT: " + subject 
				//		      + "\nMessage: " + msg + "\n";
                LOG.info("A message has been sent successfully....");
			}
			else {
              LOG.severe("Having an issue to instantiate SendEmail class....");
				resultMessage = "There is an error to instantiate SendEmail().";
			}
		}catch (Exception ex) {
			ex.printStackTrace();
			resultMessage = "There were an error: " + ex.getMessage();
		} finally {
			request.setAttribute("Message", resultMessage);
			getServletContext().getRequestDispatcher("/Result.jsp").forward(
					request, response);
		}
	}
}