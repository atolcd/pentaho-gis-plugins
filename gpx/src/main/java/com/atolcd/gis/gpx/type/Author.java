package com.atolcd.gis.gpx.type;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Author {
	
	private String name;
	private String email;
	
	public Author(String name, String email) throws AuthorException{
		this.name = name;
		this.email = checkEmail(email);
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEmail() {
		return email;
	}
	
	public void setEmail(String email) throws AuthorException {
		this.email = checkEmail(email);
	}
	
	public String getEmailId(){
		
		if(this.email !=null){
			return this.email.split("@")[0];
		}else{
			return null;
		}
	}
	
	public String getEmailDomain(){
		
		if(this.email !=null){
			return this.email.split("@")[1];
		}else{
			return null;
		}
	}
	
	private String checkEmail(String email) throws AuthorException {

		if(email != null){
			
			Pattern pattern = Pattern.compile("^.+@.+\\..+$");
			Matcher matcher = pattern.matcher(email);
			if (!matcher.matches()){
				throw new AuthorException("Wrong author mail address");
			}
			
		}
		
		return email;
	}
	
	@SuppressWarnings("serial")
	public class AuthorException extends Exception {
		
	    public AuthorException(String message) {
	        super(message);
	    }
	    
	}

}
