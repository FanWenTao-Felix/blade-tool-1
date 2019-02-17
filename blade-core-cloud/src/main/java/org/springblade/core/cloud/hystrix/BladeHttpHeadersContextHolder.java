/*
 *      Copyright (c) 2018-2028, Chill Zhuang All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the dreamlu.net developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: Chill 庄骞 (smallchill@163.com)
 */
package org.springblade.core.cloud.hystrix;

import org.springblade.core.cloud.props.BladeHystrixHeadersProperties;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.core.tool.utils.WebUtil;
import org.springframework.core.NamedThreadLocal;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.PatternMatchUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

/**
 * HttpHeadersContext
 *
 * @author Chill
 */
public class BladeHttpHeadersContextHolder {
	private static final ThreadLocal<HttpHeaders> HTTP_HEADERS_HOLDER = new NamedThreadLocal<>("Blade hystrix HttpHeaders");

	/**
	 * X-Real-IP x-forwarded-for 请求和转发的ip
	 */
	private static final String[] ALLOW_HEADS = new String[]{
		"X-Real-IP", "x-forwarded-for", "Authorization", "blade-auth"
	};

	static void set(HttpHeaders httpHeaders) {
		HTTP_HEADERS_HOLDER.set(httpHeaders);
	}

	@Nullable
	public static HttpHeaders get() {
		return HTTP_HEADERS_HOLDER.get();
	}

	static void remove() {
		HTTP_HEADERS_HOLDER.remove();
	}

	@Nullable
	public static HttpHeaders toHeaders(
		@Nullable BladeHystrixAccountGetter accountGetter,
		BladeHystrixHeadersProperties properties) {
		HttpServletRequest request = WebUtil.getRequest();
		if (request == null) {
			return null;
		}
		HttpHeaders headers = new HttpHeaders();
		String accountHeaderName = properties.getAccount();
		// 如果配置有 account 读取器
		if (accountGetter != null) {
			String xAccountHeader = accountGetter.get(request);
			if (StringUtil.isNotBlank(xAccountHeader)) {
				headers.add(accountHeaderName, xAccountHeader);
			}
		}
		List<String> allowHeadsList = new ArrayList<>(Arrays.asList(ALLOW_HEADS));
		// 如果有传递 account header 继续往下层传递
		allowHeadsList.add(accountHeaderName);
		// 传递请求头
		Enumeration<String> headerNames = request.getHeaderNames();
		if (headerNames != null) {
			List<String> allowed = properties.getAllowed();
			String pattern = properties.getPattern();
			while (headerNames.hasMoreElements()) {
				String key = headerNames.nextElement();
				// 只支持配置的 header
				if (allowHeadsList.contains(key) || allowed.contains(key) || PatternMatchUtils.simpleMatch(pattern, key)) {
					String values = request.getHeader(key);
					// header value 不为空的 传递
					if (StringUtil.isNotBlank(values)) {
						headers.add(key, values);
					}
				}
			}
		}
		return headers.isEmpty() ? null : headers;
	}
}
