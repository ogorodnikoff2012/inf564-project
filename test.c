// un exemple de fichier mini-C
// à modifier au fur et à mesure des tests
//
// la commande 'make' recompile mini-c (si nécessaire)
// et le lance sur ce fichier

int fact(int n) {
  if (n <= 1) {
    return 1;
  }
  return n * fact(n - 1);
}

int diff(int x, int y) {
  return x - y;
}

int main() {
  return diff(5, 2);
}
