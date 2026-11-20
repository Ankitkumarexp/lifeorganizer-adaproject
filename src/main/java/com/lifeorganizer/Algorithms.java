package com.lifeorganizer;
import java.util.*;
public final class Algorithms {
 private Algorithms(){}
 public static <T extends Comparable<T>> int linearSearch(List<T>a,T key){for(int i=0;i<a.size();i++)if(a.get(i).compareTo(key)==0)return i;return -1;}
 public static <T extends Comparable<T>> int binarySearch(List<T>a,T key){int l=0,r=a.size()-1;while(l<=r){int m=l+(r-l)/2,c=a.get(m).compareTo(key);if(c==0)return m;if(c<0)l=m+1;else r=m-1;}return -1;}
 public static <T extends Comparable<T>> long bubbleSort(List<T>a){long c=0;for(int i=0;i<a.size()-1;i++){boolean changed=false;for(int j=0;j<a.size()-i-1;j++){c++;if(a.get(j).compareTo(a.get(j+1))>0){Collections.swap(a,j,j+1);changed=true;}}if(!changed)break;}return c;}
 public static <T extends Comparable<T>> long selectionSort(List<T>a){long c=0;for(int i=0;i<a.size();i++){int min=i;for(int j=i+1;j<a.size();j++){c++;if(a.get(j).compareTo(a.get(min))<0)min=j;}Collections.swap(a,i,min);}return c;}
 public static <T extends Comparable<T>> long insertionSort(List<T>a){long c=0;for(int i=1;i<a.size();i++){T x=a.get(i);int j=i-1;while(j>=0){c++;if(a.get(j).compareTo(x)<=0)break;a.set(j+1,a.get(j));j--;}a.set(j+1,x);}return c;}
 public static <T extends Comparable<T>> long mergeSort(List<T>a){if(a.size()<2)return 0;int m=a.size()/2;List<T>l=new ArrayList<>(a.subList(0,m)),r=new ArrayList<>(a.subList(m,a.size()));long c=mergeSort(l)+mergeSort(r);int i=0,j=0,k=0;while(i<l.size()&&j<r.size()){c++;a.set(k++,l.get(i).compareTo(r.get(j))<=0?l.get(i++):r.get(j++));}while(i<l.size())a.set(k++,l.get(i++));while(j<r.size())a.set(k++,r.get(j++));return c;}
 public static <T extends Comparable<T>> long quickSort(List<T>a){return quick(a,0,a.size()-1);}
 private static <T extends Comparable<T>> long quick(List<T>a,int lo,int hi){if(lo>=hi)return 0;T p=a.get(lo+(hi-lo)/2);int i=lo,j=hi;long c=0;while(i<=j){while(a.get(i).compareTo(p)<0){i++;c++;}while(a.get(j).compareTo(p)>0){j--;c++;}if(i<=j){Collections.swap(a,i++,j--);c++;}}if(lo<j)c+=quick(a,lo,j);if(i<hi)c+=quick(a,i,hi);return c;}
 public static <T> PriorityQueue<T> priorityQueue(Comparator<T> comparator){return new PriorityQueue<>(comparator);}
}
