package candis.example.hash;

import candis.distributed.DistributedJobResult;

/**
 * Serializable result for HashTask.
 */
public class HashJobResult implements DistributedJobResult {

	/// Some value
	public final boolean mFoundValue;
	public final char[] mValue;

	public HashJobResult(boolean foundValue, char[] value) {
		mFoundValue = foundValue;
		mValue = value;
	}
}