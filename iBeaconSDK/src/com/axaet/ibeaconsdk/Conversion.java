package com.axaet.ibeaconsdk;

/**
 * The class is a tool for converting some of the data.Developers are not
 * available
 * 
 * @author axaet
 * 
 */
public class Conversion {
	/**
	 * Turn byte array into a string
	 * 
	 * @param src
	 * @return
	 */
	public static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	}

	/**
	 * 16 into the data system to turn the string
	 * 
	 * @param s
	 * @return
	 */
	public static String stringToHex(String s) {
		byte[] baKeyword = new byte[s.length() / 2];
		for (int i = 0; i < baKeyword.length; i++) {
			try {
				baKeyword[i] = (byte) (0xff & Integer.parseInt(
						s.substring(i * 2, i * 2 + 2), 16));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			s = new String(baKeyword, "utf-8");// UTF-16le:Not
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return s;
	}

	/**
	 * The method is generally less than
	 * 
	 * @param
	 * @return
	 */
	public static byte[] hex2Byte(String uuid) {
		byte[] data = new byte[17];
		int[] uuid_int = new int[32];
		char[] uuid_c = uuid.toCharArray();
		for (int i = 0; i < 32; i++) {
			if (uuid_c[i] == '0') {
				uuid_int[i] = 0;
			}
			if (uuid_c[i] == '1') {
				uuid_int[i] = 1;
			}
			if (uuid_c[i] == '2') {
				uuid_int[i] = 2;
			}
			if (uuid_c[i] == '3') {
				uuid_int[i] = 3;
			}
			if (uuid_c[i] == '4') {
				uuid_int[i] = 4;
			}
			if (uuid_c[i] == '5') {
				uuid_int[i] = 5;
			}
			if (uuid_c[i] == '6') {
				uuid_int[i] = 6;
			}
			if (uuid_c[i] == '7') {
				uuid_int[i] = 7;
			}
			if (uuid_c[i] == '8') {
				uuid_int[i] = 8;
			}
			if (uuid_c[i] == '9') {
				uuid_int[i] = 9;
			}
			if (uuid_c[i] == 'A' || uuid_c[i] == 'a') {
				uuid_int[i] = 10;
			}
			if (uuid_c[i] == 'B' || uuid_c[i] == 'b') {
				uuid_int[i] = 11;
			}
			if (uuid_c[i] == 'C' || uuid_c[i] == 'c') {
				uuid_int[i] = 12;
			}
			if (uuid_c[i] == 'D' || uuid_c[i] == 'd') {
				uuid_int[i] = 13;
			}
			if (uuid_c[i] == 'E' || uuid_c[i] == 'e') {
				uuid_int[i] = 14;
			}
			if (uuid_c[i] == 'F' || uuid_c[i] == 'f') {
				uuid_int[i] = 15;
			}
		}
		data[0] = (byte) 0x01;
		for (int i = 0; i < 16; i++) {
			data[i + 1] = (byte) (uuid_int[2 * i] * 16 + uuid_int[2 * i + 1]);
		}
		return data;
	}

	/**
	 * Turn the password data into an array of bytes,The method is generally
	 * less than
	 * 
	 * @param string
	 * @return
	 */
	public static byte[] str2Byte(String string, byte b) {
		char[] cs = string.toCharArray();
		byte[] bs = new byte[cs.length + 1];
		bs[0] = b;
		for (int i = 1; i <= cs.length; i++) {
			bs[i] = (byte) cs[i - 1];
		}
		return bs;
	}

	/**
	 * Turn the device name into an array of bytes,The method is generally less
	 * than
	 * 
	 * @param string
	 * @return
	 */
	public static byte[] str2ByteDeviceName(String string) {
		char[] cs = string.toCharArray();
		byte[] bs = new byte[cs.length + 2];
		bs[0] = (byte) 0x07;
		bs[1] = (byte) cs.length;
		for (int i = 2; i <= cs.length + 1; i++) {
			bs[i] = (byte) cs[i - 2];
		}
		return bs;
	}
}
