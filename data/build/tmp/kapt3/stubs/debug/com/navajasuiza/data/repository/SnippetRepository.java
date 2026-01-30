package com.navajasuiza.data.repository;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0000\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J \u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\n2\u0006\u0010\u000e\u001a\u00020\nH\u0002J\u000e\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0006J\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00060\u0013J \u0010\u0014\u001a\u0004\u0018\u00010\u00152\u0006\u0010\u0011\u001a\u00020\u00062\u0006\u0010\r\u001a\u00020\n2\u0006\u0010\u000e\u001a\u00020\nR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\u00020\u00068BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0007\u0010\b\u00a8\u0006\u0016"}, d2 = {"Lcom/navajasuiza/data/repository/SnippetRepository;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "snippetsDir", "Ljava/io/File;", "getSnippetsDir", "()Ljava/io/File;", "calculateInSampleSize", "", "options", "Landroid/graphics/BitmapFactory$Options;", "reqWidth", "reqHeight", "deleteSnippet", "", "file", "getSnippets", "", "getThumbnail", "Landroid/graphics/Bitmap;", "data_debug"})
public final class SnippetRepository {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    
    public SnippetRepository(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    private final java.io.File getSnippetsDir() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.io.File> getSnippets() {
        return null;
    }
    
    public final boolean deleteSnippet(@org.jetbrains.annotations.NotNull()
    java.io.File file) {
        return false;
    }
    
    /**
     * Efficiently loads a thumbnail for the given file.
     */
    @org.jetbrains.annotations.Nullable()
    public final android.graphics.Bitmap getThumbnail(@org.jetbrains.annotations.NotNull()
    java.io.File file, int reqWidth, int reqHeight) {
        return null;
    }
    
    private final int calculateInSampleSize(android.graphics.BitmapFactory.Options options, int reqWidth, int reqHeight) {
        return 0;
    }
}