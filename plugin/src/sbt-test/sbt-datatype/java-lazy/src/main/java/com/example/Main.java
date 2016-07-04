package com.example;

public class Main {

	public static <T> MyLazy<T> makeLazy(final T value) {
		return new MyLazy<T>() {
			public T get() {
				return value;
			}
		};
	}

	public static void main(String[] args) {
		MyLazy<Integer> lazy = new MyLazy<Integer>() { public Integer get() { return 1 / 0; } };
		B b = new B(0, 1, lazy);
		assert(b.normalField() == 0);
		assert(b.dummyField() == 1);

		// If we are not careful with lazy fields, we may encounter problem in withXXX methods.
		// Make sure that updating a field from the parent class doesn't evaluate lazy members:
		B bUpdated = b.withNormalField(1);
		assert(bUpdated.normalField() == 1);
		assert(bUpdated.dummyField() == 1);

		// Make sure that updating a field from the this class doesn't evaluate lazy members:
		B bUpdated2 = b.withDummyField(0);
		assert(bUpdated2.normalField() == 0);
		assert(bUpdated2.dummyField() == 0);

		boolean exceptionThrown = false;

		try { b.lazyField(); }
		catch (Exception e) { exceptionThrown = true; }

		assert(exceptionThrown);

	}
}