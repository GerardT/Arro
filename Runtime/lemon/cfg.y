%include {#include <assert.h>}
%include {#include <stdlib.h>}
%include {#include <string.h>}
%token_prefix TOK_
// A, B, C, L are of type 'Token'
%token_type {Token}
%extra_argument {CodeGenerator *cg}
%syntax_error  {cg->lemonError(); }
%parse_failure {cg->lemonFailure(); }
%left OR.
%left AND.

// make sure to have equivalent non-terminals on both sides to allow for correct
// application of preference rules.
// e.g. expr AND expr
line(L) ::= expr(A). { L.value = cg->rule1(L,A); }
expr(L) ::= expr(A) AND expr(B). {L.value =  cg->rule2(L,A,B); }
expr(L) ::= expr(A) OR expr(B). { L.value = cg->rule3(L,A,B); }
expr(L) ::= NAME(A) IN LPAREN states(B) RPAREN.  { L.value = cg->rule5(L,A,B); }
states(L) ::= states(A) COMMA NAME(B). { L.value = cg->rule6(L,A,B); }
states(L) ::= NAME(A). { L.value = cg->rule7(L,A);  }
