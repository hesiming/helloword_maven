package cn.retech.my_domainbean_engine.net_entitydata_tools.dream_book;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;

import cn.retech.my_domainbean_engine.net_entitydata_tools.interfaces.INetRequestEntityDataPackage;

public class NetRequestEntityDataPackageForDreamBook implements INetRequestEntityDataPackage {

	@Override
	public HttpEntity packageNetRequestEntityData(final Map<String, String> domainDD) throws Exception {

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(domainDD.size());
		Set<Entry<String, String>> entrySetOfHeaders = domainDD.entrySet();
		for (Entry<String, String> entry : entrySetOfHeaders) {
			nameValuePairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
		}

		return new UrlEncodedFormEntity(nameValuePairs,"utf-8");
	}
}
