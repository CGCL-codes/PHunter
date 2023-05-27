package analyze;

enum PatchState {
    /*
    we do not consider delete a method, if a method doesn't appear in a patched binary, it is usually like this:
-    private int readEncryptedDataHeap(ByteBuffer dst, int pos, int len) throws IOException {
+    private int readEncryptedDataHeap(ByteBuffer dst, int len) throws IOException {
    that is, modify a method's sig, we will recognize this case, mark the method as modified method
     */
    Added, Modified, Deleted
}