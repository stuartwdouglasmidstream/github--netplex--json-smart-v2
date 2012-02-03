package net.minidev.asm;

/*
 *    Copyright 2011 JSON-SMART authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Allow access reflect field using runtime generated accessor. BeansAccessor is
 * faster than java.lang.reflect.Method.invoke()
 * 
 * @author uriel Chemouni
 */
public abstract class BeansAccess {
	private HashMap<String, Accessor> map;
	// private Map<String, Accessor> map;
	private Accessor[] accs;

	protected void setAccessor(Accessor[] accs) {
		int i = 0;
		this.accs = accs;
		map = new HashMap<String, Accessor>();
		for (Accessor acc : accs) {
			acc.index = i++;
			map.put(acc.getName(), acc);
		}
	}

	public HashMap<String, Accessor> getMap() {
		return map;
	}

	public Accessor[] getAccessors() {
		return accs;
	}

	/**
	 * cache used to store built BeansAccess
	 */
	private static ConcurrentHashMap<Class<?>, BeansAccess> cache = new ConcurrentHashMap<Class<?>, BeansAccess>();

	// private final static ConcurrentHashMap<Type, AMapper<?>> cache;

	/**
	 * return the BeansAccess corresponding to a type
	 * 
	 * @param type
	 *            to be access
	 * @return the BeansAccess
	 */
	static public BeansAccess get(Class<?> type) {
		{
			BeansAccess access = cache.get(type);
			if (access != null)
				return access;
		}
		// extract all access methodes
		Accessor[] accs = ASMUtil.getAccessors(type);

		// create new class name
		String accessClassName = type.getName().concat("AccAccess");

		// extend class base loader
		DynamicClassLoader loader = new DynamicClassLoader(type.getClassLoader());
		// try to load existing class
		Class<?> accessClass = null;
		try {
			accessClass = loader.loadClass(accessClassName);
		} catch (ClassNotFoundException ignored) {
		}

		// if the class do not exists build it
		if (accessClass == null) {
			accessClass = new BeansAccessBuilder(type, accs, loader).bulid();
		}

		try {
			BeansAccess access = (BeansAccess) accessClass.newInstance();
			access.setAccessor(accs);
			cache.putIfAbsent(type, access);
			return access;
		} catch (Exception ex) {
			throw new RuntimeException("Error constructing accessor class: " + accessClassName, ex);
		}
	}

	/**
	 * set field value by field index
	 */
	abstract public void set(Object object, int methodIndex, Object value);

	/**
	 * get field value by field index
	 */
	abstract public Object get(Object object, int methodIndex);

	/**
	 * create a new targeted object
	 */
	abstract public Object newInstance();

	/**
	 * set field value by fieldname
	 */
	public void set(Object object, String methodName, Object value) {
		set(object, getIndex(methodName), value);
	}

	/**
	 * get field value by fieldname
	 */
	public Object get(Object object, String methodName) {
		return get(object, getIndex(methodName));
	}

	/**
	 * Returns the index of the field accessor.
	 */
	public int getIndex(String name) {
		Accessor ac = map.get(name);
		if (ac == null)
			return -1;
		return ac.index;
	}
}
