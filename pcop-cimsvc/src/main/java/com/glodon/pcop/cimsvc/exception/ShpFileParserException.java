package com.glodon.pcop.cimsvc.exception;


import com.glodon.pcop.cim.common.util.EnumWrapper;
import com.glodon.pcop.cimapi.exception.ApiException;

public class ShpFileParserException extends ApiException {
	private EnumWrapper.CodeAndMsg code;

	public ShpFileParserException(EnumWrapper.CodeAndMsg code, String msg) {
		super(code, msg);
		this.code = code;
	}
	public ShpFileParserException(EnumWrapper.CodeAndMsg code) {
		super(code, code.getMsg());
		this.code = code;
	}

}
