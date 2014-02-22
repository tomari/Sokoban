#!/usr/bin/perl
# map2txt,pl: Converts Gsokoban *.map files to DrSoko txt format
# * Usage: map2txt.pl a.map b.map c.map ... > soko.txt
# Stage names and file metadata needs to be modified after conversion.
# BUGS: does not handle concave surfaces
use strict;
use File::Basename 'fileparse';

# Header
print "Untitled\n";
print "Converted using map2txt.pl\n";

foreach(@ARGV) {
  open(my $src, '<', $_) or die "$!";
  my ($basename, $dirname, $ext) = fileparse($_,qr/\.[^\/]*$/);
  print "--\n";
  my $title_done=0;
  while(<$src>) {
    $_ =~ s/\n$//;
    if($_ =~ /^\s*$/) {
      next;
    } elsif($_ =~ /^\'(.+)\'$/) {
      print $1."\n";
      $title_done=1;
      next;
    }
    if(!$title_done) {
      print $basename."\n";
      $title_done=1;
    }
    my @chrs=split(//,$_);
    my $prevwall=0;
    my $inside=0;
    foreach (@chrs) {
      if($_ eq '#') {
	$prevwall=1;
	print '#';
      } else {
	#if($prevwall) {
	#  $inside=not $inside;
	#}
	if($_ eq ' ') {
	  #if($inside) {
	  if($prevwall) {
	    print '.';
	  } else {
	    print ' ';
	  }
	} elsif($_ eq '$') {
	  print 'o';
	} elsif($_ eq '.') {
	  print 'x';
	} elsif($_ eq '@') {
	  print '@';
	} elsif($_ eq '*') {
	  print '0';
	} elsif($_ eq '+') {
	  print '+';
	} else {
	  print STDERR "unknown character: ".$_."\n";
	}
	#$prevwall=0;
      }
    }
    print "\n";
  }
  close $src;
}
