package cn.retech.my_domainbean_engine.net_entitydata_tools.dream_book;

import cn.retech.my_domainbean_engine.net_entitydata_tools.interfaces.INetRespondRawEntityDataUnpack;

public final class NetRespondEntityDataUnpackDreamBook implements INetRespondRawEntityDataUnpack {

	@Override
	public String unpackNetRespondRawEntityData(final byte[] rawData) throws Exception {
		return new String(rawData, "utf-8");
	}

}
