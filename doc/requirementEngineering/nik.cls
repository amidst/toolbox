%% Identification part:

\NeedsTeXFormat{LaTeX2e}

\ProvidesClass{nik}[2007/05/22 v1.02c NIK conference document class]

%% Initial code part:


%% Options declaration part:

% Conference information (in several languages):
\DeclareOption{norsk}{\def \nik@info {Denne artikkelen ble presentert
    på konferansen NIK-\the\year; se \textsf{http://www.nik.no/}.}}
\DeclareOption{nynorsk}{\def \nik@info {Denne artikkelen vart presentert
    på konferansen NIK-\the\year; sj\aa\ \textsf{http://www.nik.no/}.}}
\DeclareOption{american}{}
\DeclareOption{english}{}
\DeclareOption{ukenglish}{}
\DeclareOption{usenglish}{}
\def \nik@info {This paper was presented at the 
  NIK-\the\year\ conference; see \textsf{http://www.nik.no/}.}

% No other options are recognized:
\DeclareOption*{\ClassWarning{nik}{Unknown option `\CurrentOption'}}

%% Options executions part:

\ProcessOptions \relax

%% Package and classloading part:

\LoadClass[a4paper,12pt]{article}

%% Main code part:

% The title should be in 16 pt bold:

\renewcommand{\title}[1]{\def 
  \@title{\normalfont\bf\fontsize{16}{19pt}\selectfont #1}}

% The author and date should be in 16 pt normal (but the default is to
% leave the date empty):

\renewcommand{\author}[1]{\def 
  \@author{\normalfont\fontsize{16}{18pt}\selectfont #1}}
\date{}
\renewcommand{\date}[1]{\def 
  \@date{\normalfont\fontsize{16}{16pt}\selectfont #1}}

% The \section should be in 16 pt bold.
% It will have less vertical specing than the original one.

\renewcommand{\section}{\@startsection{section}{1}{0pt}%
  {-3.5ex plus -1ex minus -0.2ex}%
  {0.8ex plus 0.2ex}{\normalfont\bf\fontsize{16}{16pt}\selectfont}}

% The \subsection will be in 14 pt bold, 
% also with less vertical spacing.
% It will not be numbered.

\renewcommand{\subsection}{\@startsection{subsection}{2}{0pt}%
  {-3.25ex plus -1ex minus -0.2ex}%
  {0.5ex plus 0.2ex}{\normalfont\bf\fontsize{14}{14pt}\selectfont}}
\setcounter{secnumdepth}{1}

% The \subsubsection titles should be in italics:

\renewcommand{\subsubsection}{\@startsection{subsubsection}{3}{0pt}%
  {-3.25ex plus -1ex minus -0.2ex}%
  {0.5ex plus 0.2ex}{\normalfont\it\fontsize{12}{12pt}\selectfont}}

% The abstract should not be smaller than the other text:

\renewenvironment{abstract}{%
   \begin{center}
     \textbf{\abstractname}\par
     \vspace*{-11pt}
     \begin{quotation}\noindent\ignorespaces}%
  {\end{quotation}\end{center}}

% The first page should contain a reference to the conference:

\let \nik@maketitle = \maketitle
\renewcommand{\maketitle}{\nik@maketitle
  \begingroup
    \def \@makefntext ##1{\normalsize\it ##1}
    \footnotetext{\nik@info}
  \endgroup}

% There should be no page numbers:

\renewcommand{\ps@plain}{%
  \def \@evenhead{}%
  \def \@oddhead{}%
  \def \@evenfoot{}%
  \def \@oddfoot{}}
\pagestyle{plain}

% Both left and right margins should be 3cm:

\setlength{\oddsidemargin}{-1in}
\addtolength{\oddsidemargin}{3cm}

\setlength{\textwidth}{15cm}

% Both top and bottom margins should be 2.5cm:

\setlength{\topmargin}{-1.4cm}
\setlength{\textheight}{24.8cm}

% We do not require the very tight setting usually found in books:

\pretolerance = 3000
\tolerance = 6000  \hbadness = \tolerance
