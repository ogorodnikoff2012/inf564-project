int mod(int a, int b) {
  return a - (a / b) * b;
}

int main() {
  int x;
  x = 0;

  while (x < 10) {
    if (mod(x, 2) == 0) { continue; }
    putchar(48 + x);
    x = x + 1;
  }
  putchar(10);

  return 0;
}