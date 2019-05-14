package com.ycm.demo.data.model;


public class Session {
	private int id;
    private int accountId;
    private String sign;
    private long createTime;
    
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
	public int getAccountId() {
		return accountId;
	}
	public void setAccountId(int accountId) {
		this.accountId = accountId;
	}
	public String getSign() {
		return sign;
	}
	public void setSign(String sign) {
		this.sign = sign;
	}
	public long getCreateTime() {
		return createTime;
	}
	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}
 
}
