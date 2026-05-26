# Notes <!-- omit in toc -->

<details>
<summary>Table of Contents</summary>

- [Microservices to Implement](#microservices-to-implement)
  - [Database](#database)
- [Questions](#questions)
  - [Selled Product Topics](#selled-product-topics)
- [Notas para o relatório](#notas-para-o-relatório)
- [Ver com o Daniel](#ver-com-o-daniel)
- [TODO](#todo)

</details>

## Questions Group

- [ ] Lidar com respose do get loyalty card by id porque retorna uma lista
  - [ ] Como mostramos? não era melhor juntar todas as lojas num só array e só tinhamos array para loja ao invés de array para loyalty card?
- [ ] Não estou a conseguir pôr a funcionar os declare received no loyalty card creation and read

## Questions Professor

- [ ] Selled Product pode agregar só por 1 nível (e.g.shop) ou se tem de agregar pelo nível **e** por produto

## Melhorar se houver tempo

- [ ] Mostrar os dados do form do actor 1, na user task de verificação do actor 2
- [ ] Não há forma de ser um form que não dá para alterar (quando estamos a devolver o Id criado)
- [ ] Lidar com erros `NOT_FOUND` ou assim nos delete, update, get, etc.
- [ ] Definir os assigns ao invés de estar tudo no demo

## Discount Coupon Emission

Regras:

- Ganha desconto para a loja X, se for mais do que 3 vezes à loja X no último mês
- O valor do desconto é inversamente proporcional ao número de vezes que foi à loja X no último mês
  - 5% > 10 visitas
  - 10% entre 5 e 10 visitas
  - 15% entre 3 e 5 visitas

## Cross Selling Recommendations

Regras:

- Para o cliente A, ver as lojas a que mais foi e a que menos foi no último mês
  - Caso hajam lojas empatadas (entre as mais visitadas ou as menos visitadas), escolher random um máximo de 3 lojas
  - No pior casos, ficamos com um array de 6 ids shops

## Discount Coupon Analysis

- Para um dado DiscountCoupon X
- Ver todas as compras do client daquele coupon no time frame do coupon
- Ver em quantas dessas compras o cliente usou o coupon
- Perguntar ao ollama o que é que ele acha

## Selled Product Analytics

TODO: Decide after talk with professor
