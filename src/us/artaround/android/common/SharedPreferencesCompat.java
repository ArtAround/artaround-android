package us.artaround.android.common;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * Reflection utils to call SharedPreferences$Editor.apply when possible,
 * falling back to commit when apply isn't available.
 */
public class SharedPreferencesCompat {
	private static final Method sApplyMethod = findApplyMethod();

	private static Method findApplyMethod() {
		try {
			Class<Editor> cls = SharedPreferences.Editor.class;
			return cls.getMethod("apply");
		}
		catch (NoSuchMethodException unused) {
			// fall through
		}
		return null;
	}

	public static void apply(SharedPreferences.Editor editor) {
		if (sApplyMethod != null) {
			try {
				sApplyMethod.invoke(editor);
				return;
			}
			catch (InvocationTargetException unused) {
				// fall through
			}
			catch (IllegalAccessException unused) {
				// fall through
			}
		}
		editor.commit();
	}
}